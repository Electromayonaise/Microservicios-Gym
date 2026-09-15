# Sistema de Gestión de un Gimnasio — de monolito a microservicios

Taller de Domain-Driven Design: transformar un monolito Spring Boot de gestión de gimnasio en una arquitectura de microservicios. Enunciado completo en [`docs/Statement.pdf`](docs/Statement.pdf). Presentación en [`docs/DDD-Monolito-a-Microservicios-Gimnasio.pptx`](docs/DDD-Monolito-a-Microservicios-Gimnasio.pptx).

## Contenido del repo

| Carpeta | Qué es |
|---|---|
| [`monilito-gimnasio/`](monilito-gimnasio) | El monolito original, punto de partida del taller |
| [`docs/ddd-microservicios.md`](docs/ddd-microservicios.md) | Análisis DDD: dominio, contextos acotados, agregados, value objects y diagrama de componentes |
| [`docs/DDD-Monolito-a-Microservicios-Gimnasio.pptx`](docs/DDD-Monolito-a-Microservicios-Gimnasio.pptx) | Presentación del taller |
| [`ms-membresias/`](ms-membresias) | Microservicio — contexto **Membresías** |
| [`ms-programacion/`](ms-programacion) | Microservicio — contexto **Programación** |
| [`ms-personal/`](ms-personal) | Microservicio — contexto **Personal** |
| [`ms-inventario/`](ms-inventario) | Microservicio — contexto **Inventario** |
| [`docker-compose.yml`](docker-compose.yml) | Levanta los 4 microservicios + Keycloak con un solo comando |
| [`keycloak/full-export/`](keycloak/full-export) | Configuración de Keycloak exportada (realm `gimnasio`, clientes, roles, usuarios de prueba) |
| [`postman/`](postman) | Colección de Postman con pruebas de los endpoints de cada microservicio |

## Arquitectura

Cuatro microservicios independientes, uno por contexto acotado, cada uno con su propia base de datos (H2 en memoria). El único cruce entre servicios es **Programación → Personal**: al programar una clase se valida el `entrenadorId` contra `ms-personal` vía REST (reenviando el JWT del usuario). Todos los endpoints están protegidos con JWT emitido por Keycloak y autorización basada en roles (`ROLE_ADMIN`, `ROLE_TRAINER`, `ROLE_MEMBER`).

| Microservicio | Puerto | Entidad | Endpoints | Roles permitidos |
|---|---|---|---|---|
| `ms-membresias` | `8081` | Miembro | `POST /api/miembros` | `ROLE_ADMIN` |
| | | | `GET /api/miembros` | `ROLE_ADMIN`, `ROLE_TRAINER` |
| `ms-programacion` | `8082` | Clase | `POST /api/clases` | `ROLE_ADMIN`, `ROLE_TRAINER` |
| | | | `GET /api/clases`, `GET /api/clases/{id}/entrenador` | `ROLE_ADMIN`, `ROLE_TRAINER`, `ROLE_MEMBER` |
| `ms-personal` | `8083` | Entrenador | `POST /api/entrenadores` | `ROLE_ADMIN` |
| | | | `GET /api/entrenadores`, `GET /api/entrenadores/{id}` | `ROLE_ADMIN`, `ROLE_TRAINER`, `ROLE_MEMBER` |
| `ms-inventario` | `8084` | Equipo | `POST /api/equipos` | `ROLE_ADMIN` |
| | | | `GET /api/equipos` | `ROLE_ADMIN`, `ROLE_TRAINER`, `ROLE_MEMBER` |

Diagrama de componentes completo (PlantUML) en [`docs/ddd-microservicios.md`](docs/ddd-microservicios.md#diagrama-de-componentes).

## Stack

Java 17 · Spring Boot 3.3.2 · Spring Data JPA · H2 (en memoria) · Spring Security (OAuth2 Resource Server / JWT) · Keycloak · springdoc-openapi (Swagger UI) · Maven (con wrapper `./mvnw`) · Spring `RestClient` para la comunicación entre servicios · Docker Compose.

## Seguridad: Keycloak + JWT

Cada microservicio valida los tokens JWT emitidos por un realm `gimnasio` en Keycloak (un cliente confidencial por microservicio) y autoriza cada endpoint según el rol del usuario (ver tabla de arriba). La configuración vive en:

- `src/main/java/co/analisys/<contexto>/config/SecurityConfig.java` — Spring Security como resource server JWT (Swagger UI queda público, todo lo demás requiere token).
- `src/main/java/co/analisys/<contexto>/config/KeycloakRealmRoleConverter.java` — mapea `realm_access.roles` del token a roles de Spring.
- [`keycloak/full-export/`](keycloak/full-export) — export real de Keycloak (`kc.sh export`, con los secrets reales de cada cliente y los usuarios de prueba con sus credenciales), usado por `docker-compose.yml` para reconstruir el realm cada vez que el contenedor arranca. No hay base de datos persistente: cualquier cambio manual en la consola de administración se pierde al recrear el contenedor — el realm siempre se reconstruye desde este archivo.

### Usuarios de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin1` | `Admin@2024` | `ROLE_ADMIN` |
| `trainer1` | `Trainer@2024` | `ROLE_TRAINER` |
| `member1` | `Member@2024` | `ROLE_MEMBER` |

Consola de administración de Keycloak: `http://localhost:8080` — `admin` / `admin_password`.

### Obtener un token y llamar un endpoint protegido

```bash
TOKEN=$(curl -s 'http://localhost:8080/realms/gimnasio/protocol/openid-connect/token' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=personal-service' \
  --data-urlencode 'client_secret=<ver client_secret en keycloak/full-export/gimnasio-realm.json>' \
  --data-urlencode 'username=trainer1' \
  --data-urlencode 'password=Trainer@2024' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/entrenadores
```

Casos verificados: sin header `Authorization` → `401`; token inválido/expirado → `401`; rol sin permiso para el endpoint → `403`; rol correcto → `200`.

> ⚠️ Con esta seguridad activa, la colección de Postman existente necesita agregar el header `Authorization: Bearer <token>` a cada request — ya no funciona "tal cual" contra los endpoints protegidos.

## Documentación de la API (Swagger/OpenAPI)

Cada microservicio expone su documentación sin necesidad de token en `http://localhost:<puerto>/swagger-ui/index.html` (JSON crudo en `/v3/api-docs`).

## Cómo correrlo

### Opción 1: Docker Compose (recomendada)

Construye y levanta los 4 microservicios con un solo comando:

```bash
docker compose up --build
```

`ms-programacion` espera automáticamente a que `ms-personal` esté saludable antes de arrancar (`depends_on` + healthcheck), porque valida `entrenadorId` contra ese contexto por REST. Dentro de la red de Docker se resuelven por el nombre del servicio (`http://ms-personal:8083`), no por `localhost`.

Para detener todo:

```bash
docker compose down
```

### Opción 2: Maven, cada microservicio por separado

Cada microservicio es un proyecto Maven independiente y arranca solo (cada uno trae su propio `DataLoader` con datos de ejemplo). Para que la validación REST de `ms-programacion` funcione, levanta primero `ms-personal`:

```bash
# terminal 1
cd ms-personal && ./mvnw spring-boot:run

# terminal 2
cd ms-programacion && ./mvnw spring-boot:run

# terminal 3 y 4 (sin dependencias)
cd ms-membresias && ./mvnw spring-boot:run
cd ms-inventario && ./mvnw spring-boot:run
```

En Windows usa `mvnw.cmd` en lugar de `./mvnw`. Keycloak sigue necesitando correr en Docker (`docker compose up -d keycloak`); si los microservicios corren fuera de Docker, cambia `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` en cada `application.properties` de `keycloak:8080` a `localhost:8080`.

### Probar los endpoints

Con los 4 microservicios y Keycloak arriba (por cualquiera de las dos opciones), primero obtén un token (ver sección de Seguridad) y luego:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8082/api/clases/1/entrenador
```

O importa [`postman/Gimnasio-Microservicios.postman_collection.json`](postman/Gimnasio-Microservicios.postman_collection.json) en Postman — trae una carpeta por microservicio con casos válidos y casos que verifican las invariantes de dominio (email inválido/duplicado, capacidad y cantidad negativas, especialidad fuera de catálogo, etc.). También se puede correr desde la terminal con [newman](https://github.com/postmanlabs/newman):

```bash
newman run postman/Gimnasio-Microservicios.postman_collection.json
```

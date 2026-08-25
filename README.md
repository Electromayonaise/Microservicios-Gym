# Sistema de Gestión de un Gimnasio — de monolito a microservicios

Taller de Domain-Driven Design: transformar un monolito Spring Boot de gestión de gimnasio en una arquitectura de microservicios. Enunciado completo en [`docs/Statement.pdf`](docs/Statement.pdf).

## Contenido del repo

| Carpeta | Qué es |
|---|---|
| [`monilito-gimnasio/`](monilito-gimnasio) | El monolito original, punto de partida del taller |
| [`docs/ddd-microservicios.md`](docs/ddd-microservicios.md) | Análisis DDD: dominio, contextos acotados, agregados y diagrama de componentes |
| [`ms-membresias/`](ms-membresias) | Microservicio — contexto **Membresías** |
| [`ms-programacion/`](ms-programacion) | Microservicio — contexto **Programación** |
| [`ms-personal/`](ms-personal) | Microservicio — contexto **Personal** |
| [`ms-inventario/`](ms-inventario) | Microservicio — contexto **Inventario** |

## Arquitectura

Cuatro microservicios independientes, uno por contexto acotado, cada uno con su propia base de datos (H2 en memoria). El único cruce entre servicios es **Programación → Personal**: al programar una clase se valida el `entrenadorId` contra `ms-personal` vía REST.

| Microservicio | Puerto | Entidad | Endpoints |
|---|---|---|---|
| `ms-membresias` | `8081` | Miembro | `POST/GET /api/miembros` |
| `ms-programacion` | `8082` | Clase | `POST/GET /api/clases`, `GET /api/clases/{id}/entrenador` |
| `ms-personal` | `8083` | Entrenador | `POST/GET /api/entrenadores`, `GET /api/entrenadores/{id}` |
| `ms-inventario` | `8084` | Equipo | `POST/GET /api/equipos` |

Diagrama de componentes completo (PlantUML) en [`docs/ddd-microservicios.md`](docs/ddd-microservicios.md#diagrama-de-componentes).

## Stack

Java 17 · Spring Boot 3.3.2 · Spring Data JPA · H2 (en memoria) · Maven (con wrapper `./mvnw`) · Spring `RestClient` para la comunicación entre servicios.

## Cómo correrlo

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

En Windows usa `mvnw.cmd` en lugar de `./mvnw`.

Prueba rápida una vez arriba `ms-personal` y `ms-programacion`:

```bash
curl http://localhost:8082/api/clases/1/entrenador
```

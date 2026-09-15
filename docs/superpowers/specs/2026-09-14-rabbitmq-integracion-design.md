# Diseño: Comunicación asíncrona con RabbitMQ (Parte 2 del taller)

Fecha: 2026-09-14
Estado: propuesto, pendiente de revisión del usuario

## Contexto y objetivo

El [enunciado de la Parte 2](../../Statement.pdf) del taller "Comunicación y Seguridad en
Microservicios" pide, sobre el proyecto de gimnasio (4 microservicios, ya con JWT/Keycloak/Swagger
de la Parte 1 — commit `02727f8`):

1. Configurar RabbitMQ para el proyecto.
2. Notificaciones asincrónicas para nuevas inscripciones de miembros.
3. Patrón publish/subscribe para eventos importantes (ej. cambios en el horario de clases).
4. Dead Letter Queue para mensajes fallidos en el procesamiento de pagos.

Como referencia de implementación real se estudió `A:\Deployments\Microservices-Library`
(proyecto de biblioteca) y el enunciado que se usó ahí ("4_RabbitMQ entre microservicio-circulacion
y microservicio-notificacion"). Puntos clave de esa referencia que determinan las decisiones de
este diseño:

- `microservicio-notificacion` ya existía en la biblioteca **antes** del ejercicio de RabbitMQ —
  RabbitMQ se añadió sobre una relación productor/consumidor que ya existía (circulación llamaba a
  notificación por Feign; se reemplazó por publish/subscribe). No se creó un microservicio nuevo
  para el ejercicio.
- El patrón real es: agregar `spring-boot-starter-amqp` a un servicio productor y a un servicio
  consumidor ya existentes, declarar `RabbitMQConfig` en cada lado (productor: Queue + TopicExchange
  + Binding + `RabbitTemplate`; consumidor: Queue + `SimpleRabbitListenerContainerFactory`), inyectar
  `RabbitTemplate` en un método de negocio ya existente y sustituir (o convivir con) la llamada
  síncrona anterior, y agregar un `@RabbitListener` que delega en la lógica de negocio existente del
  consumidor.
- El DTO del mensaje (`NotificacionDTO`) se duplica literalmente en ambos servicios — funciona ahí
  porque los 4 microservicios de la biblioteca comparten el mismo paquete base
  (`co.analisys.biblioteca`), así que la clase duplicada tiene el mismo FQN en ambos lados, y
  `Jackson2JsonMessageConverter` puede resolver el tipo por el header `__TypeId__` sin configuración
  extra.
- El enunciado de esa referencia declara explícitamente que DLQ y manejo avanzado de errores quedan
  fuera de alcance — no hay nada que copiar para el punto 4.

**Consecuencia para este diseño:** el proyecto de gimnasio no tiene un microservicio de
notificaciones preexistente, y sus 4 microservicios usan paquetes base distintos
(`co.analisys.membresias`, `.programacion`, `.personal`, `.inventario`). Por lo tanto:

- No se crea ningún microservicio nuevo. Los tres flujos se implementan como retrofits sobre
  servicios ya existentes, igual que en la referencia.
- El punto 2 (inscripciones) se resuelve dentro de un solo servicio (`ms-membresias` publica y
  consume su propio evento) porque no hay ningún otro contexto con una razón de negocio real para
  reaccionar a una inscripción nueva.
- El punto 3 (pub/sub de horario) sí cruza dos servicios reales (`ms-programacion` → `ms-personal`,
  el mismo cruce que ya existe hoy vía REST para validar `entrenadorId`), y por eso sí choca con el
  problema de FQN distinto entre paquetes — se resuelve explícitamente más abajo.
- El punto 4 (DLQ de pagos) es diseño original: no existe hoy un concepto de "pago" en el dominio;
  se modela lo mínimo necesario para demostrar el mecanismo de DLQ, sin persistirlo como agregado.

Se confirmó en chat con el usuario: no agregar un 5º microservicio, no agregar un cliente de
Keycloak nuevo, seguir la mecánica de la referencia donde aplique (retrofit, no infraestructura
nueva), y agregar cobertura de Postman/newman para RabbitMQ y para los endpoints nuevos.

## Alcance

Tres flujos de mensajería sobre los microservicios existentes, más la infraestructura y las pruebas
que los acompañan:

| # | Flujo | Productor | Consumidor(es) |
|---|---|---|---|
| 1 | Notificación de inscripción | `ms-membresias` | `ms-membresias` (mismo servicio) |
| 2 | Pub/sub cambio de horario | `ms-programacion` | `ms-personal` |
| 3 | DLQ de pagos fallidos | `ms-membresias` | `ms-membresias` (cola normal + DLQ) |

Fuera de alcance: persistir pagos como agregado de dominio, reintentos automáticos desde la DLQ,
UI de administración de RabbitMQ más allá de la consola de gestión que trae la imagen oficial,
tracing/observabilidad de mensajes.

## Infraestructura: RabbitMQ en Docker Compose

Se agrega un servicio `rabbitmq` a `docker-compose.yml`, imagen `rabbitmq:3.13-management` (incluye
el plugin de management API/UI que se usa también desde Postman), con healthcheck
`rabbitmq-diagnostics -q ping`, puertos `5672` (AMQP) y `15672` (consola/API de gestión), y
credenciales por defecto `guest`/`guest` (suficiente para un entorno de taller, igual que el resto
del proyecto no usa secretos productivos para Keycloak).

`ms-membresias`, `ms-programacion` y `ms-personal` (los tres servicios involucrados en algún flujo
de mensajería) declaran `depends_on: rabbitmq: condition: service_healthy` y reciben
`SPRING_RABBITMQ_HOST=rabbitmq` por variable de entorno, igual patrón que ya usa
`PERSONAL_SERVICE_URL` hoy. `ms-inventario` no participa en ningún flujo y no cambia.

Cada uno de los tres `pom.xml` agrega `spring-boot-starter-amqp` (única dependencia nueva, igual
que en la referencia). `application.properties` de cada uno agrega:

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

(la propiedad `host` se sobreescribe a `rabbitmq` vía la variable de entorno `SPRING_RABBITMQ_HOST`
cuando corre en Docker Compose, igual que ya pasa con `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`.)

## Flujo 1 — Notificación asíncrona de inscripción (`ms-membresias`)

- **Exchange:** `membresias.exchange` (`TopicExchange`).
- **Cola:** `notificacion.inscripcion.queue`.
- **Routing key:** `miembro.inscrito`.
- **DTO del mensaje:** `co.analisys.membresias.messaging.dto.InscripcionNotificacionDTO` —
  `miembroId`, `nombre`, `email`, `fechaInscripcion`. Vive en un solo servicio, sin problema de FQN.
- `RabbitMQConfig` en `co.analisys.membresias.config` declara Queue + TopicExchange + Binding +
  `Jackson2JsonMessageConverter` + `RabbitTemplate` + `SimpleRabbitListenerContainerFactory` (todo
  en una sola clase, porque el mismo servicio es productor y consumidor).
- `MiembroService.registrarMiembro(...)`: después de `miembroRepository.save(miembro)`, publica el
  evento con `rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, dto)`. El endpoint
  `POST /api/miembros` no cambia su contrato ni su código de respuesta.
- Nuevo `NotificacionInscripcionListener` con `@RabbitListener(queues = "notificacion.inscripcion.queue")`
  que simula el envío de la notificación (log), igual de "stub" que `NotificacionService.enviarNotificacion`
  en la referencia.

## Flujo 2 — Publish/subscribe: cambio de horario de clases (`ms-programacion` → `ms-personal`)

- **Exchange:** `programacion.exchange` (`TopicExchange`), declarado en ambos servicios (cada lado
  declara sus propios beans, igual que hace la referencia con `notificacion.exchange`).
- **Cola (en `ms-personal`):** `horario.clase.queue`, bindeada a `programacion.exchange` con routing
  key `clase.horario.cambiado`.
- **Nuevo comportamiento de dominio en `ms-programacion`:** `Clase` gana un método de instancia
  `reprogramar(LocalDateTime nuevoHorario)` que valida `nuevoHorario != null` (mismo estilo de
  invariante que ya usa `programar(...)`). Nuevo endpoint `PATCH /api/clases/{id}/horario`
  (`hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER')`, igual que `POST /api/clases`), que llama a
  `ClaseService.cambiarHorario(claseId, nuevoHorario)`: carga la clase, llama `clase.reprogramar(...)`,
  guarda, y publica el evento.
- **DTO del evento — el punto que sí requiere una decisión explícita:** a diferencia del flujo 1,
  este cruza `ms-programacion` y `ms-personal`, que tienen paquetes base distintos
  (`co.analisys.programacion` vs `co.analisys.personal`). Duplicar la clase tal cual hace la
  referencia (mismo nombre, cada uno en su propio paquete base) produciría FQNs distintos, y
  `Jackson2JsonMessageConverter` fallaría al resolver el tipo por el header `__TypeId__` por
  defecto. Para mantenerse fiel al patrón de la referencia (duplicar el DTO, sin librería
  compartida) sin ese problema, la clase de evento se duplica bajo el **mismo paquete literal en
  ambos servicios**, `co.analisys.gimnasio.eventos.HorarioClaseCambiadoEvento` — un paquete que no
  coincide con el paquete base de ningún servicio individual, usado únicamente para los DTOs de
  eventos que cruzan servicios. Esto conserva el FQN idéntico en ambos lados (la condición real
  que hace funcionar el truco en la referencia) sin introducir un módulo/librería compartida ni
  tipos inferidos por configuración. Campos: `claseId`, `nombreClase`, `horarioAnterior`,
  `horarioNuevo`, `entrenadorId`.
- **Consumidor en `ms-personal`:** nuevo `HorarioClaseListener` con
  `@RabbitListener(queues = "horario.clase.queue")` que simula notificar al entrenador afectado
  (log), igual de "stub" que el resto de los consumidores de este diseño y de la referencia.
- `ms-programacion` solo declara el `TopicExchange` y el `RabbitTemplate` (es productor puro, no
  necesita cola propia). `ms-personal` declara el `TopicExchange`, la `Queue` y el `Binding` (más
  el converter y el listener container factory).

## Flujo 3 — Dead Letter Queue en procesamiento de pagos (`ms-membresias`)

No hay nada equivalente en la referencia ni en su enunciado (lo declaran explícitamente fuera de
alcance), así que este flujo es diseño original, acotado a lo mínimo que demuestra el mecanismo:

- **Concepto de pago:** no se modela como agregado persistente (no hay entidad `Pago`, no hay
  tabla) — es deliberadamente mínimo, solo lo necesario para tener un mensaje que pueda fallar y
  caer a la DLQ. Nuevo endpoint `POST /api/miembros/{id}/pagos`
  (`hasAnyRole('ROLE_ADMIN', 'ROLE_MEMBER')`, un miembro puede pagar su propia membresía y un admin
  puede registrar el pago por él) recibe `{ monto: BigDecimal, concepto: String }`, valida que el
  miembro exista, publica el mensaje y responde `202 Accepted` de inmediato (el procesamiento es
  asíncrono).
- **Exchange:** `pagos.exchange` (`DirectExchange` — a diferencia de los flujos anteriores es
  estrictamente punto a punto, sin necesidad de wildcards de topic).
- **Cola principal:** `pagos.procesar.queue`, bindeada con routing key `pago.procesar`, declarada
  con los argumentos `x-dead-letter-exchange=pagos.dlx` y `x-dead-letter-routing-key=pago.fallido`.
- **Cola de dead-letter:** `pagos.dlq`, en el exchange `pagos.dlx` (`DirectExchange`), routing key
  `pago.fallido`.
- **Consumidor principal:** `PagoProcesadorListener` en `pagos.procesar.queue` valida `monto != null
  && monto.compareTo(BigDecimal.ZERO) > 0`; si es inválido, lanza
  `AmqpRejectAndDontRequeueException` (fuerza el dead-lettering sin reintentar indefinidamente); si
  es válido, loguea el pago como procesado (stub, sin pasarela real).
- **Consumidor de la DLQ:** `PagoFallidoListener` en `pagos.dlq` loguea el mensaje fallido como
  alerta para seguimiento manual — demuestra que la DLQ efectivamente captura el mensaje rechazado.
- **DTO:** `co.analisys.membresias.messaging.dto.PagoDTO` (`miembroId`, `monto`, `concepto`) — vive
  en un solo servicio, sin problema de FQN, igual que el flujo 1.

## Pruebas de Postman / newman

La colección existente (`postman/Gimnasio-Microservicios.postman_collection.json`) ya cubre
Keycloak/JWT (carpetas `0. Auth` y `1. Seguridad (JWT)`) y ya se corre con
`newman run postman/Gimnasio-Microservicios.postman_collection.json`. Se extiende así:

**Keycloak — cobertura de los 2 endpoints nuevos**, agregada a la carpeta existente
`1. Seguridad (JWT)`, mismo estilo que los casos actuales (sin token → 401, rol sin permiso → 403,
rol correcto → 200/202):
- `PATCH /api/clases/{id}/horario` sin token → 401; con `member_token` → 403 (requiere ADMIN o
  TRAINER).
- `POST /api/miembros/{id}/pagos` sin token → 401; con `trainer_token` → 403 (requiere ADMIN o
  MEMBER).

**RabbitMQ — nueva carpeta `2. RabbitMQ`**, con 3 variables de colección nuevas
(`rabbitmq_mgmt_url=http://localhost:15672`, `rabbitmq_user=guest`, `rabbitmq_password=guest`) y,
por cada flujo, un request que dispara la acción HTTP seguido de un request que consulta la
Management HTTP API de RabbitMQ (`GET {{rabbitmq_mgmt_url}}/api/queues/%2F/<cola>` con basic auth)
para verificar que el mensaje efectivamente se movió por la cola esperada — mismo patrón que la
carpeta "RabbitMQ" de la colección de Postman de la referencia (que también consulta la Management
API tras una acción). Los requests de verificación llevan un *delay* de request (≈1500 ms,
configurado en `protocolProfileBehavior`/`Settings` de cada request, que newman respeta) para dar
tiempo al listener a procesar antes de leer las estadísticas de la cola:

- **Inscripción:** `POST /api/miembros` (ADMIN, email único generado con `{{$randomEmail}}`) → 201;
  luego `GET .../queues/%2F/notificacion.inscripcion.queue` → assert `messages_ready == 0` (ya
  consumido) y `message_stats.deliver_get.count >= 1`.
- **Horario:** `PATCH /api/clases/{id}/horario` (TRAINER) → 200; luego
  `GET .../queues/%2F/horario.clase.queue` → mismo assert que arriba.
- **DLQ — camino feliz:** `POST /api/miembros/{id}/pagos` con `monto` válido → 202; luego
  `GET .../queues/%2F/pagos.procesar.queue` → `message_stats.deliver_get.count >= 1` y
  `GET .../queues/%2F/pagos.dlq` → `messages_ready` sin cambios respecto a su valor antes del test.
- **DLQ — camino de falla:** `POST /api/miembros/{id}/pagos` con `monto` negativo → 202 (la
  validación de negocio es asíncrona, el endpoint solo encola); luego
  `GET .../queues/%2F/pagos.dlq` → `messages_ready >= 1`, confirmando que el mensaje inválido cayó
  a la dead letter queue.

El `README.md` se actualiza con una sección de RabbitMQ (consola de gestión, credenciales, cómo
levantar el servicio) y la mención de la carpeta nueva de Postman; el comando de newman no cambia
(mismo archivo de colección).

## Fuera de alcance / decisiones explícitas

- No se crea ningún microservicio nuevo ni cliente de Keycloak nuevo.
- No se introduce ninguna librería/módulo compartido entre servicios; el único acoplamiento nuevo
  es el paquete literal `co.analisys.gimnasio.eventos` duplicado en `ms-programacion` y
  `ms-personal` para el evento de horario (ver Flujo 2), que es la mínima variación necesaria sobre
  el patrón de la referencia para que funcione con paquetes base distintos.
- El pago no se persiste como agregado; el diseño se limita a lo que demuestra el mecanismo de DLQ.
- No hay reintentos automáticos desde la DLQ ni republicación a la cola principal — el consumidor de
  la DLQ solo loguea, igual de "stub" que el resto de los consumidores de este taller.

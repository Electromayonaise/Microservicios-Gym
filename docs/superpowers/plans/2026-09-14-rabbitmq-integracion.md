# RabbitMQ Integration (Parte 2 del taller) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add asynchronous RabbitMQ messaging to the gym microservices project — inscription notifications, a publish/subscribe schedule-change event, and a Dead Letter Queue for failed payments — plus Postman/newman coverage, without introducing any new microservice or Keycloak client.

**Architecture:** RabbitMQ runs as a new Docker Compose service. `ms-membresias` is self-contained producer+consumer for member enrollment notifications and for the payment/DLQ flow. `ms-programacion` (producer) and `ms-personal` (consumer) share a `TopicExchange` for the class-schedule-changed pub/sub event, using a deliberately shared literal package (`co.analisys.gimnasio.eventos`) for the event DTO so `Jackson2JsonMessageConverter`'s default `__TypeId__` type resolution works across two services with different base packages. `ms-inventario` is untouched.

**Tech Stack:** Spring Boot 3.3.2, Java 17, Spring AMQP (`spring-boot-starter-amqp`), RabbitMQ 3.13 (management image), Docker Compose, Postman/newman.

**Spec:** [docs/superpowers/specs/2026-09-14-rabbitmq-integracion-design.md](../specs/2026-09-14-rabbitmq-integracion-design.md)

## Global Constraints

- No new microservice and no new Keycloak client — all three flows retrofit onto the 4 existing microservices (`ms-inventario` is not touched by any flow).
- Only new dependency per involved service: `spring-boot-starter-amqp` (`ms-membresias`, `ms-programacion`, `ms-personal`).
- No shared library/module between services. The one deliberate exception is the event DTO for the schedule-change flow, duplicated verbatim under the exact same package literal (`co.analisys.gimnasio.eventos`) in both `ms-programacion` and `ms-personal`, so the FQN matches on both sides — this is the minimum change needed to make the reference project's "duplicate the DTO" pattern work when base packages differ.
- RabbitMQ credentials are the image defaults `guest`/`guest` (workshop-grade, matches how Keycloak already uses non-production secrets in this repo).
- Follow existing code conventions exactly: field injection with `@Autowired` (not constructor injection), `System.out.println(...)` for stub/simulated logging (matches `DataLoader` classes and the reference project's `NotificacionService`, no logging framework is used anywhere in this repo), Lombok `@Getter`/`@NoArgsConstructor(access = AccessLevel.PROTECTED)` style for entities, `record` for DTOs, `@PreAuthorize("hasRole(...)")`/`hasAnyRole(...)` at controller level, `ResponseStatusException` for HTTP error responses in services.
- Payments are not persisted as a domain aggregate — no `Pago` entity, no table. The DLQ flow is intentionally minimal.
- Never add a "Co-Authored-By: Claude" or similar AI co-author trailer to any git commit message.
- Windows/PowerShell is the primary shell for this repo; verification commands below use `.\mvnw.cmd`.

---

### Task 1: RabbitMQ infrastructure — Docker Compose, dependency, properties

**Files:**
- Modify: `docker-compose.yml`
- Modify: `ms-membresias/pom.xml`
- Modify: `ms-programacion/pom.xml`
- Modify: `ms-personal/pom.xml`
- Modify: `ms-membresias/src/main/resources/application.properties`
- Modify: `ms-programacion/src/main/resources/application.properties`
- Modify: `ms-personal/src/main/resources/application.properties`

**Interfaces:**
- Produces: a running `rabbitmq` container reachable at `localhost:5672` (AMQP) and `localhost:15672` (management UI/API, `guest`/`guest`) when run standalone, or at `rabbitmq:5672` from inside the other containers when run via `docker compose up`. Every later task's `RabbitMQConfig` classes connect using these properties/env vars.

- [ ] **Step 1: Add `spring-boot-starter-amqp` to `ms-membresias/pom.xml`**

Edit `ms-membresias/pom.xml`, inserting the new dependency between the `h2` and `lombok` dependencies:

```xml
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-amqp</artifactId>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
```

- [ ] **Step 2: Repeat Step 1 for `ms-programacion/pom.xml` and `ms-personal/pom.xml`**

Same edit (identical `h2`/lombok surrounding block exists verbatim in both files).

- [ ] **Step 3: Add RabbitMQ connection properties to the three `application.properties` files**

Append to the end of `ms-membresias/src/main/resources/application.properties`:

```properties

spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

Append the same block to `ms-programacion/src/main/resources/application.properties` and `ms-personal/src/main/resources/application.properties`.

- [ ] **Step 4: Add the `rabbitmq` service to `docker-compose.yml`**

Append after the `keycloak` service block (end of file):

```yaml

  # RabbitMQ - broker de mensajeria para las notificaciones asincronas de
  # inscripcion, el patron publish/subscribe de cambios de horario y la
  # Dead Letter Queue de pagos fallidos (ver
  # docs/superpowers/specs/2026-09-14-rabbitmq-integracion-design.md). La
  # imagen "-management" trae el plugin de consola/API de gestion en el
  # puerto 15672, usado tambien desde la coleccion de Postman para
  # verificar que los mensajes efectivamente pasaron por cada cola.
  rabbitmq:
    image: rabbitmq:3.13-management
    container_name: rabbitmq_gym_app
    ports:
      - "5672:5672"
      - "15672:15672"
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 5s
      timeout: 5s
      retries: 10
      start_period: 20s
```

- [ ] **Step 5: Wire `ms-personal` to depend on `rabbitmq`**

Edit the `ms-personal` service block:

```yaml
  ms-personal:
    build: ./ms-personal
    ports:
      - "8083:8083"
    environment:
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      rabbitmq:
        condition: service_healthy
    healthcheck:
```

(keep the existing `healthcheck:` block below unchanged.)

- [ ] **Step 6: Wire `ms-membresias` to depend on `rabbitmq`**

Same edit shape as Step 5, applied to the `ms-membresias` service block:

```yaml
  ms-membresias:
    build: ./ms-membresias
    ports:
      - "8081:8081"
    environment:
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      rabbitmq:
        condition: service_healthy
    healthcheck:
```

- [ ] **Step 7: Wire `ms-programacion` to depend on `rabbitmq` (in addition to `ms-personal`)**

Edit the existing `ms-programacion` `environment:`/`depends_on:` block:

```yaml
    environment:
      # Dentro de la red de docker-compose, ms-personal se resuelve por el
      # nombre del servicio en vez de localhost.
      PERSONAL_SERVICE_URL: http://ms-personal:8083
      SPRING_RABBITMQ_HOST: rabbitmq
    depends_on:
      ms-personal:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
```

- [ ] **Step 8: Validate the compose file and dependency resolution**

Run: `docker compose config`
Expected: prints the fully resolved compose configuration with no errors, `rabbitmq` service present, and `SPRING_RABBITMQ_HOST=rabbitmq` under `ms-membresias`, `ms-programacion`, `ms-personal`.

Run (each service directory): `.\mvnw.cmd -q -DskipTests compile`
Expected: `BUILD SUCCESS` for `ms-membresias`, `ms-programacion`, `ms-personal` (the new dependency resolves and nothing else changed yet, so compilation must still succeed).

- [ ] **Step 9: Commit**

```bash
git add docker-compose.yml ms-membresias/pom.xml ms-programacion/pom.xml ms-personal/pom.xml \
  ms-membresias/src/main/resources/application.properties \
  ms-programacion/src/main/resources/application.properties \
  ms-personal/src/main/resources/application.properties
git commit -m "Add RabbitMQ infrastructure to docker-compose and the 3 involved microservices"
```

---

### Task 2: Flujo 1 — Notificación asíncrona de inscripción (`ms-membresias`)

**Files:**
- Create: `ms-membresias/src/main/java/co/analisys/membresias/messaging/dto/InscripcionNotificacionDTO.java`
- Create: `ms-membresias/src/main/java/co/analisys/membresias/config/RabbitMQConfig.java`
- Create: `ms-membresias/src/main/java/co/analisys/membresias/messaging/NotificacionInscripcionListener.java`
- Modify: `ms-membresias/src/main/java/co/analisys/membresias/service/MiembroService.java`
- Modify: `postman/Gimnasio-Microservicios.postman_collection.json`

**Interfaces:**
- Consumes: `MiembroRepository.save`, `Miembro.registrar` (existing, unchanged).
- Produces: `RabbitMQConfig.MEMBRESIAS_EXCHANGE` (`"membresias.exchange"`), `RabbitMQConfig.NOTIFICACION_INSCRIPCION_QUEUE` (`"notificacion.inscripcion.queue"`), `RabbitMQConfig.MIEMBRO_INSCRITO_ROUTING_KEY` (`"miembro.inscrito"`) — these three constants are reused by Task 4 when it extends this same `RabbitMQConfig` class with the payment/DLQ beans.

- [ ] **Step 1: Create the message DTO**

Create `ms-membresias/src/main/java/co/analisys/membresias/messaging/dto/InscripcionNotificacionDTO.java`:

```java
package co.analisys.membresias.messaging.dto;

import java.time.LocalDate;

public record InscripcionNotificacionDTO(Long miembroId, String nombre, String email, LocalDate fechaInscripcion) {
}
```

- [ ] **Step 2: Create the RabbitMQ configuration**

Create `ms-membresias/src/main/java/co/analisys/membresias/config/RabbitMQConfig.java`:

```java
package co.analisys.membresias.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MEMBRESIAS_EXCHANGE = "membresias.exchange";
    public static final String NOTIFICACION_INSCRIPCION_QUEUE = "notificacion.inscripcion.queue";
    public static final String MIEMBRO_INSCRITO_ROUTING_KEY = "miembro.inscrito";

    @Bean
    public TopicExchange membresiasExchange() {
        return new TopicExchange(MEMBRESIAS_EXCHANGE);
    }

    @Bean
    public Queue notificacionInscripcionQueue() {
        return new Queue(NOTIFICACION_INSCRIPCION_QUEUE, true);
    }

    @Bean
    public Binding notificacionInscripcionBinding(Queue notificacionInscripcionQueue, TopicExchange membresiasExchange) {
        return BindingBuilder.bind(notificacionInscripcionQueue).to(membresiasExchange).with(MIEMBRO_INSCRITO_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
```

- [ ] **Step 3: Publish the event from `MiembroService.registrarMiembro`**

Modify `ms-membresias/src/main/java/co/analisys/membresias/service/MiembroService.java` — replace the whole file:

```java
package co.analisys.membresias.service;

import co.analisys.membresias.config.RabbitMQConfig;
import co.analisys.membresias.messaging.dto.InscripcionNotificacionDTO;
import co.analisys.membresias.model.Email;
import co.analisys.membresias.model.Miembro;
import co.analisys.membresias.repository.MiembroRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MiembroService {
    @Autowired
    private MiembroRepository miembroRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public Miembro registrarMiembro(String nombre, String email) {
        Email emailValidado = new Email(email);
        if (miembroRepository.existsByEmail(emailValidado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un miembro registrado con el email " + emailValidado.getValor());
        }
        Miembro miembro = Miembro.registrar(nombre, emailValidado);
        miembro = miembroRepository.save(miembro);

        InscripcionNotificacionDTO notificacion = new InscripcionNotificacionDTO(
                miembro.getId(), miembro.getNombre(), miembro.getEmail().getValor(), miembro.getFechaInscripcion());
        rabbitTemplate.convertAndSend(RabbitMQConfig.MEMBRESIAS_EXCHANGE, RabbitMQConfig.MIEMBRO_INSCRITO_ROUTING_KEY, notificacion);

        return miembro;
    }

    public List<Miembro> obtenerTodosMiembros() {
        return miembroRepository.findAll();
    }
}
```

- [ ] **Step 4: Create the consumer**

Create `ms-membresias/src/main/java/co/analisys/membresias/messaging/NotificacionInscripcionListener.java`:

```java
package co.analisys.membresias.messaging;

import co.analisys.membresias.config.RabbitMQConfig;
import co.analisys.membresias.messaging.dto.InscripcionNotificacionDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificacionInscripcionListener {

    @RabbitListener(queues = RabbitMQConfig.NOTIFICACION_INSCRIPCION_QUEUE)
    public void recibirNotificacion(InscripcionNotificacionDTO notificacion) {
        System.out.println("Notificacion de inscripcion enviada a " + notificacion.email()
                + ": bienvenido/a al gimnasio, " + notificacion.nombre() + " (miembroId=" + notificacion.miembroId() + ")");
    }
}
```

- [ ] **Step 5: Compile check**

Run (from `ms-membresias/`): `.\mvnw.cmd -q -DskipTests compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Add RabbitMQ management variables and the "2. RabbitMQ" folder to the Postman collection**

Open `postman/Gimnasio-Microservicios.postman_collection.json`.

Add three new entries to the top-level `variable` array, right after `member_token`:

```json
    { "key": "admin_token", "value": "" },
    { "key": "trainer_token", "value": "" },
    { "key": "member_token", "value": "" },
    { "key": "rabbitmq_mgmt_url", "value": "http://localhost:15672" },
    { "key": "rabbitmq_user", "value": "guest" },
    { "key": "rabbitmq_password", "value": "guest" }
```

Add a new top-level folder right after the `"1. Seguridad (JWT)"` folder closes (i.e. right before the `"name": "ms-membresias"` folder starts):

```json
    {
      "name": "2. RabbitMQ",
      "description": "Dispara cada flujo de mensajeria y verifica contra la Management HTTP API de RabbitMQ (puerto 15672) que el mensaje efectivamente proceso por la cola esperada. Requiere que ademas de los microservicios y Keycloak este arriba el contenedor 'rabbitmq' (ver docker-compose.yml). Cada request de verificacion espera ~1.5s en su pre-request script para dar tiempo al listener a procesar antes de leer las estadisticas de la cola.",
      "item": [
        {
          "name": "POST Registrar miembro - dispara notificacion de inscripcion",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Authorization", "value": "Bearer {{admin_token}}" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n    \"nombre\": \"Rabbit Test\",\n    \"email\": \"rabbit.test+{{$timestamp}}@email.com\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": {
              "raw": "{{ms_membresias_url}}/api/miembros",
              "host": ["{{ms_membresias_url}}"],
              "path": ["api", "miembros"]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 200 (miembro registrado)', function () {",
                  "    pm.response.to.have.status(200);",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "GET Verificar cola notificacion.inscripcion.queue proceso el mensaje",
          "request": {
            "method": "GET",
            "auth": {
              "type": "basic",
              "basic": [
                { "key": "username", "value": "{{rabbitmq_user}}" },
                { "key": "password", "value": "{{rabbitmq_password}}" }
              ]
            },
            "header": [],
            "url": {
              "raw": "{{rabbitmq_mgmt_url}}/api/queues/%2F/notificacion.inscripcion.queue",
              "host": ["{{rabbitmq_mgmt_url}}"],
              "path": ["api", "queues", "%2F", "notificacion.inscripcion.queue"]
            }
          },
          "event": [
            {
              "listen": "prerequest",
              "script": {
                "type": "text/javascript",
                "exec": ["setTimeout(function () {}, 1500);"]
              }
            },
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 200 (cola existe)', function () {",
                  "    pm.response.to.have.status(200);",
                  "});",
                  "const body = pm.response.json();",
                  "pm.test('El listener ya proceso el mensaje (no quedan pendientes)', function () {",
                  "    pm.expect(body.messages_ready).to.eql(0);",
                  "});",
                  "pm.test('La cola entrego al menos un mensaje al listener', function () {",
                  "    pm.expect(body.message_stats.deliver_get.count).to.be.at.least(1);",
                  "});"
                ]
              }
            }
          ]
        }
      ]
    },
```

- [ ] **Step 7: Start RabbitMQ and ms-membresias, verify manually**

Run: `docker compose up -d rabbitmq ms-membresias keycloak`
Wait for both to report healthy: `docker compose ps`

Get a token and register a member (bash):

```bash
TOKEN=$(curl -s 'http://localhost:8080/realms/gimnasio/protocol/openid-connect/token' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=membresias-service' \
  --data-urlencode 'client_secret=07ec89b0cc5e358114c08e9fd7e20ee6' \
  --data-urlencode 'username=admin1' \
  --data-urlencode 'password=Admin@2024' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

curl -s -X POST http://localhost:8081/api/miembros \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nombre":"Verificacion Manual","email":"verificacion.manual@email.com"}'

curl -s -u guest:guest http://localhost:15672/api/queues/%2F/notificacion.inscripcion.queue | python3 -m json.tool
```

Expected: the POST returns the created `Miembro` with `id`; the queue GET shows `"messages_ready": 0` and `"message_stats"` present with `"deliver_get": {"count": 1, ...}`. Also check `docker compose logs ms-membresias` for the `"Notificacion de inscripcion enviada a ..."` line.

- [ ] **Step 8: Run the Postman collection with newman**

Run: `newman run postman/Gimnasio-Microservicios.postman_collection.json`
Expected: all requests pass, including the new `"2. RabbitMQ"` folder.

Run: `docker compose down`

- [ ] **Step 9: Commit**

```bash
git add ms-membresias/src/main/java/co/analisys/membresias/messaging \
  ms-membresias/src/main/java/co/analisys/membresias/config/RabbitMQConfig.java \
  ms-membresias/src/main/java/co/analisys/membresias/service/MiembroService.java \
  postman/Gimnasio-Microservicios.postman_collection.json
git commit -m "Add async enrollment notification via RabbitMQ in ms-membresias"
```

---

### Task 3: Flujo 2 — Publish/subscribe cambio de horario (`ms-programacion` → `ms-personal`)

**Files:**
- Create: `ms-programacion/src/main/java/co/analisys/gimnasio/eventos/HorarioClaseCambiadoEvento.java`
- Create: `ms-personal/src/main/java/co/analisys/gimnasio/eventos/HorarioClaseCambiadoEvento.java`
- Create: `ms-programacion/src/main/java/co/analisys/programacion/config/RabbitMQConfig.java`
- Create: `ms-personal/src/main/java/co/analisys/personal/config/RabbitMQConfig.java`
- Create: `ms-personal/src/main/java/co/analisys/personal/messaging/HorarioClaseListener.java`
- Create: `ms-programacion/src/main/java/co/analisys/programacion/dto/CambioHorarioRequest.java`
- Modify: `ms-programacion/src/main/java/co/analisys/programacion/model/Clase.java`
- Modify: `ms-programacion/src/main/java/co/analisys/programacion/service/ClaseService.java`
- Modify: `ms-programacion/src/main/java/co/analisys/programacion/controller/ClaseController.java`
- Modify: `postman/Gimnasio-Microservicios.postman_collection.json`

**Interfaces:**
- Consumes: `Clase` fields/getters from Lombok `@Getter` (`getId()`, `getNombre()`, `getHorario()`, `getEntrenadorId()`), `EntrenadorId.getValor()` (returns `Long`) — all pre-existing.
- Produces: `Clase.reprogramar(LocalDateTime nuevoHorario)` (new instance method, used only by `ClaseService.cambiarHorario`), `ClaseService.cambiarHorario(ClaseId id, LocalDateTime nuevoHorario)` returning `Clase` (used by `ClaseController`), `co.analisys.gimnasio.eventos.HorarioClaseCambiadoEvento` (record: `claseId`, `nombreClase`, `horarioAnterior`, `horarioNuevo`, `entrenadorId`) consumed by `ms-personal`'s listener.

- [ ] **Step 1: Create the shared event DTO in `ms-programacion`**

Create `ms-programacion/src/main/java/co/analisys/gimnasio/eventos/HorarioClaseCambiadoEvento.java`:

```java
package co.analisys.gimnasio.eventos;

import java.time.LocalDateTime;

/**
 * Evento de mensajeria compartido entre ms-programacion (productor) y
 * ms-personal (consumidor) para el cambio de horario de una clase. Vive en
 * el mismo paquete literal en ambos microservicios (no en el paquete base
 * de ninguno) para que Jackson2JsonMessageConverter resuelva el tipo por
 * el header __TypeId__ sin configuracion adicional -- ver la seccion
 * "Flujo 2" de docs/superpowers/specs/2026-09-14-rabbitmq-integracion-design.md.
 */
public record HorarioClaseCambiadoEvento(Long claseId, String nombreClase, LocalDateTime horarioAnterior,
                                          LocalDateTime horarioNuevo, Long entrenadorId) {
}
```

- [ ] **Step 2: Duplicate the exact same file in `ms-personal`**

Create `ms-personal/src/main/java/co/analisys/gimnasio/eventos/HorarioClaseCambiadoEvento.java` with byte-for-byte identical content to Step 1.

- [ ] **Step 3: Add `Clase.reprogramar(...)`**

Modify `ms-programacion/src/main/java/co/analisys/programacion/model/Clase.java` — insert a new instance method right after the `programar(...)` static factory method and before the closing `}` of the class:

```java
    public void reprogramar(LocalDateTime nuevoHorario) {
        if (nuevoHorario == null) {
            throw new IllegalArgumentException("El nuevo horario no puede ser nulo");
        }
        this.horario = nuevoHorario;
    }
```

- [ ] **Step 4: Create the request DTO**

Create `ms-programacion/src/main/java/co/analisys/programacion/dto/CambioHorarioRequest.java`:

```java
package co.analisys.programacion.dto;

import java.time.LocalDateTime;

public record CambioHorarioRequest(LocalDateTime nuevoHorario) {
}
```

- [ ] **Step 5: Create the producer-side RabbitMQ configuration**

Create `ms-programacion/src/main/java/co/analisys/programacion/config/RabbitMQConfig.java`:

```java
package co.analisys.programacion.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PROGRAMACION_EXCHANGE = "programacion.exchange";
    public static final String CLASE_HORARIO_CAMBIADO_ROUTING_KEY = "clase.horario.cambiado";

    @Bean
    public TopicExchange programacionExchange() {
        return new TopicExchange(PROGRAMACION_EXCHANGE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
```

- [ ] **Step 6: Publish the event from `ClaseService.cambiarHorario`**

Modify `ms-programacion/src/main/java/co/analisys/programacion/service/ClaseService.java` — replace the whole file:

```java
package co.analisys.programacion.service;

import co.analisys.gimnasio.eventos.HorarioClaseCambiadoEvento;
import co.analisys.programacion.client.PersonalClient;
import co.analisys.programacion.config.RabbitMQConfig;
import co.analisys.programacion.dto.ClaseDetalleDTO;
import co.analisys.programacion.dto.ClaseRequest;
import co.analisys.programacion.dto.EntrenadorDTO;
import co.analisys.programacion.model.Capacidad;
import co.analisys.programacion.model.Clase;
import co.analisys.programacion.model.ClaseId;
import co.analisys.programacion.model.EntrenadorId;
import co.analisys.programacion.repository.ClaseRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ClaseService {
    @Autowired
    private ClaseRepository claseRepository;
    @Autowired
    private PersonalClient personalClient;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    public Clase programarClase(ClaseRequest request) {
        if (request.entrenadorId() == null) {
            throw new IllegalArgumentException("La clase debe asignarse a un entrenador (entrenadorId)");
        }
        EntrenadorId entrenadorId = new EntrenadorId(request.entrenadorId());
        personalClient.obtenerEntrenador(entrenadorId);
        Clase clase = Clase.programar(request.nombre(), request.horario(),
                new Capacidad(request.capacidadMaxima()), entrenadorId);
        return claseRepository.save(clase);
    }

    public List<Clase> obtenerTodasClases() {
        return claseRepository.findAll();
    }

    public ClaseDetalleDTO obtenerClaseConEntrenador(ClaseId id) {
        Clase clase = claseRepository.findById(id.valor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clase no encontrada: " + id.valor()));
        EntrenadorDTO entrenador = personalClient.obtenerEntrenador(clase.getEntrenadorId());
        return new ClaseDetalleDTO(clase.getId(), clase.getNombre(), clase.getHorario(),
                clase.getCapacidadMaxima().getValor(), entrenador);
    }

    public Clase cambiarHorario(ClaseId id, LocalDateTime nuevoHorario) {
        Clase clase = claseRepository.findById(id.valor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clase no encontrada: " + id.valor()));
        LocalDateTime horarioAnterior = clase.getHorario();
        clase.reprogramar(nuevoHorario);
        clase = claseRepository.save(clase);

        HorarioClaseCambiadoEvento evento = new HorarioClaseCambiadoEvento(clase.getId(), clase.getNombre(),
                horarioAnterior, clase.getHorario(), clase.getEntrenadorId().getValor());
        rabbitTemplate.convertAndSend(RabbitMQConfig.PROGRAMACION_EXCHANGE, RabbitMQConfig.CLASE_HORARIO_CAMBIADO_ROUTING_KEY, evento);

        return clase;
    }
}
```

- [ ] **Step 7: Add the `PATCH /api/clases/{id}/horario` endpoint**

Modify `ms-programacion/src/main/java/co/analisys/programacion/controller/ClaseController.java` — replace the whole file:

```java
package co.analisys.programacion.controller;

import co.analisys.programacion.dto.CambioHorarioRequest;
import co.analisys.programacion.dto.ClaseDetalleDTO;
import co.analisys.programacion.dto.ClaseRequest;
import co.analisys.programacion.model.Clase;
import co.analisys.programacion.model.ClaseId;
import co.analisys.programacion.service.ClaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Programación", description = "Programación de clases del gimnasio")
@RestController
@RequestMapping("/api/clases")
public class ClaseController {
    @Autowired
    private ClaseService claseService;

    @Operation(
        summary = "Programar una clase",
        description = "Programa una nueva clase asignada a un entrenador existente (valida el entrenadorId contra ms-personal).")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER')")
    public Clase programarClase(@RequestBody ClaseRequest request) {
        return claseService.programarClase(request);
    }

    @Operation(
        summary = "Consultar todas las clases",
        description = "Obtiene la lista de todas las clases programadas.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER', 'ROLE_MEMBER')")
    public List<Clase> obtenerTodasClases() {
        return claseService.obtenerTodasClases();
    }

    @Operation(
        summary = "Consultar una clase con su entrenador",
        description = "Obtiene el detalle de una clase junto con los datos de su entrenador asignado (consulta a ms-personal).")
    @GetMapping("/{id}/entrenador")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER', 'ROLE_MEMBER')")
    public ClaseDetalleDTO obtenerClaseConEntrenador(
            @Parameter(description = "Identificador de la clase") @PathVariable Long id) {
        return claseService.obtenerClaseConEntrenador(new ClaseId(id));
    }

    @Operation(
        summary = "Cambiar el horario de una clase",
        description = "Reprograma una clase existente y publica un evento pub/sub (clase.horario.cambiado) para que ms-personal notifique al entrenador asignado.")
    @PatchMapping("/{id}/horario")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER')")
    public Clase cambiarHorario(
            @Parameter(description = "Identificador de la clase") @PathVariable Long id,
            @RequestBody CambioHorarioRequest request) {
        return claseService.cambiarHorario(new ClaseId(id), request.nuevoHorario());
    }
}
```

- [ ] **Step 8: Create the consumer-side RabbitMQ configuration in `ms-personal`**

Create `ms-personal/src/main/java/co/analisys/personal/config/RabbitMQConfig.java`:

```java
package co.analisys.personal.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PROGRAMACION_EXCHANGE = "programacion.exchange";
    public static final String HORARIO_CLASE_QUEUE = "horario.clase.queue";
    public static final String CLASE_HORARIO_CAMBIADO_ROUTING_KEY = "clase.horario.cambiado";

    @Bean
    public TopicExchange programacionExchange() {
        return new TopicExchange(PROGRAMACION_EXCHANGE);
    }

    @Bean
    public Queue horarioClaseQueue() {
        return new Queue(HORARIO_CLASE_QUEUE, true);
    }

    @Bean
    public Binding horarioClaseBinding(Queue horarioClaseQueue, TopicExchange programacionExchange) {
        return BindingBuilder.bind(horarioClaseQueue).to(programacionExchange).with(CLASE_HORARIO_CAMBIADO_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}
```

- [ ] **Step 9: Create the consumer**

Create `ms-personal/src/main/java/co/analisys/personal/messaging/HorarioClaseListener.java`:

```java
package co.analisys.personal.messaging;

import co.analisys.gimnasio.eventos.HorarioClaseCambiadoEvento;
import co.analisys.personal.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class HorarioClaseListener {

    @RabbitListener(queues = RabbitMQConfig.HORARIO_CLASE_QUEUE)
    public void onHorarioCambiado(HorarioClaseCambiadoEvento evento) {
        System.out.println("Notificando a entrenadorId=" + evento.entrenadorId() + ": la clase '"
                + evento.nombreClase() + "' (id=" + evento.claseId() + ") cambio de horario de "
                + evento.horarioAnterior() + " a " + evento.horarioNuevo());
    }
}
```

- [ ] **Step 10: Compile check**

Run (from `ms-programacion/`): `.\mvnw.cmd -q -DskipTests compile`
Run (from `ms-personal/`): `.\mvnw.cmd -q -DskipTests compile`
Expected: `BUILD SUCCESS` for both.

- [ ] **Step 11: Add JWT 401/403 cases for the new endpoint**

Open `postman/Gimnasio-Microservicios.postman_collection.json`. Inside the `"1. Seguridad (JWT)"` folder's `item` array, add two entries right after the last existing entry (`"POST Programar clase con rol MEMBER -> 403 (requiere ADMIN o TRAINER)"`), before the folder's closing `]`:

```json
        {
          "name": "PATCH Cambiar horario de clase SIN token -> 401",
          "request": {
            "method": "PATCH",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "body": {
              "mode": "raw",
              "raw": "{\n    \"nuevoHorario\": \"2026-09-20T09:00:00\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": {
              "raw": "{{ms_programacion_url}}/api/clases/1/horario",
              "host": ["{{ms_programacion_url}}"],
              "path": ["api", "clases", "1", "horario"]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 401 (sin credenciales)', function () {",
                  "    pm.response.to.have.status(401);",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "PATCH Cambiar horario de clase con rol MEMBER -> 403 (requiere ADMIN o TRAINER)",
          "request": {
            "method": "PATCH",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Authorization", "value": "Bearer {{member_token}}" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n    \"nuevoHorario\": \"2026-09-20T09:00:00\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": {
              "raw": "{{ms_programacion_url}}/api/clases/1/horario",
              "host": ["{{ms_programacion_url}}"],
              "path": ["api", "clases", "1", "horario"]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 403 (rol MEMBER sin permiso)', function () {",
                  "    pm.response.to.have.status(403);",
                  "});"
                ]
              }
            }
          ]
        }
```

- [ ] **Step 12: Add the schedule-change pair to the "2. RabbitMQ" folder**

Inside `"2. RabbitMQ"` → `item`, append after the inscripción pair added in Task 2, before the folder's closing `]`:

```json
        {
          "name": "PATCH Cambiar horario de clase - dispara evento pub/sub",
          "request": {
            "method": "PATCH",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Authorization", "value": "Bearer {{trainer_token}}" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n    \"nuevoHorario\": \"2026-09-20T09:00:00\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": {
              "raw": "{{ms_programacion_url}}/api/clases/1/horario",
              "host": ["{{ms_programacion_url}}"],
              "path": ["api", "clases", "1", "horario"]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 200 (horario actualizado)', function () {",
                  "    pm.response.to.have.status(200);",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "GET Verificar cola horario.clase.queue proceso el mensaje",
          "request": {
            "method": "GET",
            "auth": {
              "type": "basic",
              "basic": [
                { "key": "username", "value": "{{rabbitmq_user}}" },
                { "key": "password", "value": "{{rabbitmq_password}}" }
              ]
            },
            "header": [],
            "url": {
              "raw": "{{rabbitmq_mgmt_url}}/api/queues/%2F/horario.clase.queue",
              "host": ["{{rabbitmq_mgmt_url}}"],
              "path": ["api", "queues", "%2F", "horario.clase.queue"]
            }
          },
          "event": [
            {
              "listen": "prerequest",
              "script": {
                "type": "text/javascript",
                "exec": ["setTimeout(function () {}, 1500);"]
              }
            },
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 200 (cola existe)', function () {",
                  "    pm.response.to.have.status(200);",
                  "});",
                  "const body = pm.response.json();",
                  "pm.test('El listener ya proceso el mensaje (no quedan pendientes)', function () {",
                  "    pm.expect(body.messages_ready).to.eql(0);",
                  "});",
                  "pm.test('La cola entrego al menos un mensaje al listener', function () {",
                  "    pm.expect(body.message_stats.deliver_get.count).to.be.at.least(1);",
                  "});"
                ]
              }
            }
          ]
        }
```

- [ ] **Step 13: Start the full stack and verify manually**

Run: `docker compose up -d`
Wait for all services healthy: `docker compose ps`

```bash
TOKEN=$(curl -s 'http://localhost:8080/realms/gimnasio/protocol/openid-connect/token' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=programacion-service' \
  --data-urlencode 'client_secret=160ae73fb9cf40e9cc1fc7fbb6a94322' \
  --data-urlencode 'username=trainer1' \
  --data-urlencode 'password=Trainer@2024' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

curl -s -X PATCH http://localhost:8082/api/clases/1/horario \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nuevoHorario":"2026-09-25T10:00:00"}'

curl -s -u guest:guest http://localhost:15672/api/queues/%2F/horario.clase.queue | python3 -m json.tool
```

Expected: the PATCH returns the updated `Clase` with `horario: "2026-09-25T10:00:00"`; the queue GET shows `"messages_ready": 0` and `"deliver_get": {"count": 1, ...}`; `docker compose logs ms-personal` shows the `"Notificando a entrenadorId=..."` line.

- [ ] **Step 14: Run the Postman collection with newman**

Run: `newman run postman/Gimnasio-Microservicios.postman_collection.json`
Expected: all requests pass.

Run: `docker compose down`

- [ ] **Step 15: Commit**

```bash
git add ms-programacion ms-personal/src/main/java/co/analisys/gimnasio \
  ms-personal/src/main/java/co/analisys/personal/config/RabbitMQConfig.java \
  ms-personal/src/main/java/co/analisys/personal/messaging \
  postman/Gimnasio-Microservicios.postman_collection.json
git commit -m "Add publish/subscribe class schedule-change event between ms-programacion and ms-personal"
```

---

### Task 4: Flujo 3 — Dead Letter Queue en procesamiento de pagos (`ms-membresias`)

**Files:**
- Create: `ms-membresias/src/main/java/co/analisys/membresias/messaging/dto/PagoDTO.java`
- Create: `ms-membresias/src/main/java/co/analisys/membresias/dto/PagoRequest.java`
- Create: `ms-membresias/src/main/java/co/analisys/membresias/messaging/PagoProcesadorListener.java`
- Create: `ms-membresias/src/main/java/co/analisys/membresias/messaging/PagoFallidoListener.java`
- Modify: `ms-membresias/src/main/java/co/analisys/membresias/config/RabbitMQConfig.java`
- Modify: `ms-membresias/src/main/java/co/analisys/membresias/service/MiembroService.java`
- Modify: `ms-membresias/src/main/java/co/analisys/membresias/controller/MiembroController.java`
- Modify: `postman/Gimnasio-Microservicios.postman_collection.json`

**Interfaces:**
- Consumes: `RabbitMQConfig` class and `RabbitTemplate rabbitTemplate` field from Task 2 (same file/service, extended here).
- Produces: `RabbitMQConfig.PAGOS_EXCHANGE`, `RabbitMQConfig.PAGOS_DLX`, `RabbitMQConfig.PAGOS_PROCESAR_QUEUE`, `RabbitMQConfig.PAGOS_DLQ`, `RabbitMQConfig.PAGO_PROCESAR_ROUTING_KEY`, `RabbitMQConfig.PAGO_FALLIDO_ROUTING_KEY`; `MiembroService.registrarPago(Long miembroId, BigDecimal monto, String concepto)` (void); `co.analisys.membresias.messaging.dto.PagoDTO` (record: `miembroId`, `monto`, `concepto`).

- [ ] **Step 1: Create the message DTO and the request DTO**

Create `ms-membresias/src/main/java/co/analisys/membresias/messaging/dto/PagoDTO.java`:

```java
package co.analisys.membresias.messaging.dto;

import java.math.BigDecimal;

public record PagoDTO(Long miembroId, BigDecimal monto, String concepto) {
}
```

Create `ms-membresias/src/main/java/co/analisys/membresias/dto/PagoRequest.java`:

```java
package co.analisys.membresias.dto;

import java.math.BigDecimal;

public record PagoRequest(BigDecimal monto, String concepto) {
}
```

- [ ] **Step 2: Extend `RabbitMQConfig` with the payment exchange, queue and DLQ**

Modify `ms-membresias/src/main/java/co/analisys/membresias/config/RabbitMQConfig.java`. Update the imports (add `DirectExchange` and `QueueBuilder`) and add the new constants and beans.

Replace:

```java
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
```

with:

```java
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
```

Replace:

```java
    public static final String MEMBRESIAS_EXCHANGE = "membresias.exchange";
    public static final String NOTIFICACION_INSCRIPCION_QUEUE = "notificacion.inscripcion.queue";
    public static final String MIEMBRO_INSCRITO_ROUTING_KEY = "miembro.inscrito";
```

with:

```java
    public static final String MEMBRESIAS_EXCHANGE = "membresias.exchange";
    public static final String NOTIFICACION_INSCRIPCION_QUEUE = "notificacion.inscripcion.queue";
    public static final String MIEMBRO_INSCRITO_ROUTING_KEY = "miembro.inscrito";

    public static final String PAGOS_EXCHANGE = "pagos.exchange";
    public static final String PAGOS_DLX = "pagos.dlx";
    public static final String PAGOS_PROCESAR_QUEUE = "pagos.procesar.queue";
    public static final String PAGOS_DLQ = "pagos.dlq";
    public static final String PAGO_PROCESAR_ROUTING_KEY = "pago.procesar";
    public static final String PAGO_FALLIDO_ROUTING_KEY = "pago.fallido";
```

Add these new `@Bean` methods right after `notificacionInscripcionBinding(...)` and before `messageConverter()`:

```java
    @Bean
    public DirectExchange pagosExchange() {
        return new DirectExchange(PAGOS_EXCHANGE);
    }

    @Bean
    public DirectExchange pagosDlx() {
        return new DirectExchange(PAGOS_DLX);
    }

    @Bean
    public Queue pagosProcesarQueue() {
        return QueueBuilder.durable(PAGOS_PROCESAR_QUEUE)
                .withArgument("x-dead-letter-exchange", PAGOS_DLX)
                .withArgument("x-dead-letter-routing-key", PAGO_FALLIDO_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue pagosDlq() {
        return new Queue(PAGOS_DLQ, true);
    }

    @Bean
    public Binding pagosProcesarBinding(Queue pagosProcesarQueue, DirectExchange pagosExchange) {
        return BindingBuilder.bind(pagosProcesarQueue).to(pagosExchange).with(PAGO_PROCESAR_ROUTING_KEY);
    }

    @Bean
    public Binding pagosDlqBinding(Queue pagosDlq, DirectExchange pagosDlx) {
        return BindingBuilder.bind(pagosDlq).to(pagosDlx).with(PAGO_FALLIDO_ROUTING_KEY);
    }
```

- [ ] **Step 3: Add `MiembroService.registrarPago(...)`**

Modify `ms-membresias/src/main/java/co/analisys/membresias/service/MiembroService.java` — add imports and the new method.

Replace:

```java
import co.analisys.membresias.config.RabbitMQConfig;
import co.analisys.membresias.messaging.dto.InscripcionNotificacionDTO;
import co.analisys.membresias.model.Email;
import co.analisys.membresias.model.Miembro;
import co.analisys.membresias.repository.MiembroRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
```

with:

```java
import co.analisys.membresias.config.RabbitMQConfig;
import co.analisys.membresias.messaging.dto.InscripcionNotificacionDTO;
import co.analisys.membresias.messaging.dto.PagoDTO;
import co.analisys.membresias.model.Email;
import co.analisys.membresias.model.Miembro;
import co.analisys.membresias.repository.MiembroRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
```

Add this method right after `obtenerTodosMiembros()`, before the closing `}` of the class:

```java

    public void registrarPago(Long miembroId, BigDecimal monto, String concepto) {
        if (!miembroRepository.existsById(miembroId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro no encontrado: " + miembroId);
        }
        PagoDTO pago = new PagoDTO(miembroId, monto, concepto);
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAGOS_EXCHANGE, RabbitMQConfig.PAGO_PROCESAR_ROUTING_KEY, pago);
    }
```

- [ ] **Step 4: Add `POST /api/miembros/{id}/pagos`**

Modify `ms-membresias/src/main/java/co/analisys/membresias/controller/MiembroController.java` — replace the whole file:

```java
package co.analisys.membresias.controller;

import co.analisys.membresias.dto.MiembroRequest;
import co.analisys.membresias.dto.PagoRequest;
import co.analisys.membresias.model.Miembro;
import co.analisys.membresias.service.MiembroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Membresías", description = "Registro y consulta de miembros del gimnasio")
@RestController
@RequestMapping("/api/miembros")
public class MiembroController {
    @Autowired
    private MiembroService miembroService;

    @Operation(
        summary = "Registrar un miembro",
        description = "Registra un nuevo miembro del gimnasio a partir de su nombre y correo electrónico.")
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Miembro registrarMiembro(@RequestBody MiembroRequest request) {
        return miembroService.registrarMiembro(request.nombre(), request.email());
    }

    @Operation(
        summary = "Consultar todos los miembros",
        description = "Obtiene la lista de todos los miembros registrados en el gimnasio.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER')")
    public List<Miembro> obtenerTodosMiembros() {
        return miembroService.obtenerTodosMiembros();
    }

    @Operation(
        summary = "Registrar un pago",
        description = "Encola el pago de un miembro para procesamiento asincrono via RabbitMQ. Un monto invalido cae a la Dead Letter Queue de pagos.")
    @PostMapping("/{id}/pagos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MEMBER')")
    public void registrarPago(
            @Parameter(description = "Identificador del miembro") @PathVariable Long id,
            @RequestBody PagoRequest request) {
        miembroService.registrarPago(id, request.monto(), request.concepto());
    }
}
```

- [ ] **Step 5: Create the payment processor and DLQ listeners**

Create `ms-membresias/src/main/java/co/analisys/membresias/messaging/PagoProcesadorListener.java`:

```java
package co.analisys.membresias.messaging;

import co.analisys.membresias.config.RabbitMQConfig;
import co.analisys.membresias.messaging.dto.PagoDTO;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PagoProcesadorListener {

    @RabbitListener(queues = RabbitMQConfig.PAGOS_PROCESAR_QUEUE)
    public void procesarPago(PagoDTO pago) {
        if (pago.monto() == null || pago.monto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AmqpRejectAndDontRequeueException(
                    "Monto de pago invalido para miembroId=" + pago.miembroId() + ": " + pago.monto());
        }
        System.out.println("Pago procesado para miembroId=" + pago.miembroId()
                + ": " + pago.monto() + " (" + pago.concepto() + ")");
    }
}
```

Create `ms-membresias/src/main/java/co/analisys/membresias/messaging/PagoFallidoListener.java`:

```java
package co.analisys.membresias.messaging;

import co.analisys.membresias.config.RabbitMQConfig;
import co.analisys.membresias.messaging.dto.PagoDTO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PagoFallidoListener {

    @RabbitListener(queues = RabbitMQConfig.PAGOS_DLQ)
    public void registrarPagoFallido(PagoDTO pago) {
        System.out.println("ALERTA: pago rechazado para miembroId=" + pago.miembroId()
                + ", monto=" + pago.monto() + ", concepto=" + pago.concepto() + " -- requiere seguimiento manual");
    }
}
```

- [ ] **Step 6: Compile check**

Run (from `ms-membresias/`): `.\mvnw.cmd -q -DskipTests compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Add JWT 401/403 cases for the new endpoint**

Inside `"1. Seguridad (JWT)"` → `item`, append after the two entries added in Task 3, before the folder's closing `]`:

```json
        {
          "name": "POST Registrar pago SIN token -> 401",
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "body": {
              "mode": "raw",
              "raw": "{\n    \"monto\": 50000,\n    \"concepto\": \"Mensualidad\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": {
              "raw": "{{ms_membresias_url}}/api/miembros/1/pagos",
              "host": ["{{ms_membresias_url}}"],
              "path": ["api", "miembros", "1", "pagos"]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 401 (sin credenciales)', function () {",
                  "    pm.response.to.have.status(401);",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "POST Registrar pago con rol TRAINER -> 403 (requiere ADMIN o MEMBER)",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Authorization", "value": "Bearer {{trainer_token}}" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n    \"monto\": 50000,\n    \"concepto\": \"Mensualidad\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": {
              "raw": "{{ms_membresias_url}}/api/miembros/1/pagos",
              "host": ["{{ms_membresias_url}}"],
              "path": ["api", "miembros", "1", "pagos"]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 403 (rol TRAINER sin permiso)', function () {",
                  "    pm.response.to.have.status(403);",
                  "});"
                ]
              }
            }
          ]
        }
```

- [ ] **Step 8: Add the happy-path and DLQ-failure pairs to the "2. RabbitMQ" folder**

Inside `"2. RabbitMQ"` → `item`, append after the schedule-change pair added in Task 3, before the folder's closing `]`:

```json
        {
          "name": "POST Registrar pago valido - se procesa sin caer a la DLQ",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Authorization", "value": "Bearer {{member_token}}" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n    \"monto\": 50000,\n    \"concepto\": \"Mensualidad\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": {
              "raw": "{{ms_membresias_url}}/api/miembros/1/pagos",
              "host": ["{{ms_membresias_url}}"],
              "path": ["api", "miembros", "1", "pagos"]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 202 (pago encolado)', function () {",
                  "    pm.response.to.have.status(202);",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "GET Verificar pago valido no cayo a la DLQ",
          "request": {
            "method": "GET",
            "auth": {
              "type": "basic",
              "basic": [
                { "key": "username", "value": "{{rabbitmq_user}}" },
                { "key": "password", "value": "{{rabbitmq_password}}" }
              ]
            },
            "header": [],
            "url": {
              "raw": "{{rabbitmq_mgmt_url}}/api/queues/%2F/pagos.dlq",
              "host": ["{{rabbitmq_mgmt_url}}"],
              "path": ["api", "queues", "%2F", "pagos.dlq"]
            }
          },
          "event": [
            {
              "listen": "prerequest",
              "script": {
                "type": "text/javascript",
                "exec": ["setTimeout(function () {}, 1500);"]
              }
            },
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 200 (cola DLQ existe)', function () {",
                  "    pm.response.to.have.status(200);",
                  "});",
                  "const body = pm.response.json();",
                  "pm.test('El pago valido no genero mensajes en la DLQ', function () {",
                  "    pm.expect(body.messages_ready).to.eql(0);",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "POST Registrar pago con monto invalido - cae a la DLQ",
          "request": {
            "method": "POST",
            "header": [
              { "key": "Content-Type", "value": "application/json" },
              { "key": "Authorization", "value": "Bearer {{member_token}}" }
            ],
            "body": {
              "mode": "raw",
              "raw": "{\n    \"monto\": -100,\n    \"concepto\": \"Mensualidad\"\n}",
              "options": { "raw": { "language": "json" } }
            },
            "url": {
              "raw": "{{ms_membresias_url}}/api/miembros/1/pagos",
              "host": ["{{ms_membresias_url}}"],
              "path": ["api", "miembros", "1", "pagos"]
            }
          },
          "event": [
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 202 (pago encolado, la validacion de monto es asincrona)', function () {",
                  "    pm.response.to.have.status(202);",
                  "});"
                ]
              }
            }
          ]
        },
        {
          "name": "GET Verificar pago invalido cayo a la DLQ",
          "request": {
            "method": "GET",
            "auth": {
              "type": "basic",
              "basic": [
                { "key": "username", "value": "{{rabbitmq_user}}" },
                { "key": "password", "value": "{{rabbitmq_password}}" }
              ]
            },
            "header": [],
            "url": {
              "raw": "{{rabbitmq_mgmt_url}}/api/queues/%2F/pagos.dlq",
              "host": ["{{rabbitmq_mgmt_url}}"],
              "path": ["api", "queues", "%2F", "pagos.dlq"]
            }
          },
          "event": [
            {
              "listen": "prerequest",
              "script": {
                "type": "text/javascript",
                "exec": ["setTimeout(function () {}, 1500);"]
              }
            },
            {
              "listen": "test",
              "script": {
                "type": "text/javascript",
                "exec": [
                  "pm.test('Status code es 200 (cola DLQ existe)', function () {",
                  "    pm.response.to.have.status(200);",
                  "});",
                  "const body = pm.response.json();",
                  "pm.test('El pago invalido quedo en la Dead Letter Queue', function () {",
                  "    pm.expect(body.messages_ready).to.be.at.least(1);",
                  "});"
                ]
              }
            }
          ]
        }
```

- [ ] **Step 9: Start the full stack and verify manually**

Run: `docker compose up -d`
Wait for all services healthy: `docker compose ps`

```bash
TOKEN=$(curl -s 'http://localhost:8080/realms/gimnasio/protocol/openid-connect/token' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=membresias-service' \
  --data-urlencode 'client_secret=07ec89b0cc5e358114c08e9fd7e20ee6' \
  --data-urlencode 'username=member1' \
  --data-urlencode 'password=Member@2024' \
  | python3 -c "import json,sys; print(json.load(sys.stdin)['access_token'])")

# camino feliz
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8081/api/miembros/1/pagos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"monto":50000,"concepto":"Mensualidad"}'

# camino de falla -> DLQ
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8081/api/miembros/1/pagos \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"monto":-100,"concepto":"Mensualidad"}'

curl -s -u guest:guest http://localhost:15672/api/queues/%2F/pagos.procesar.queue | python3 -m json.tool
curl -s -u guest:guest http://localhost:15672/api/queues/%2F/pagos.dlq | python3 -m json.tool
```

Expected: both POSTs return `202`; `pagos.procesar.queue` shows `deliver_get.count >= 2` and `messages_ready: 0`; `pagos.dlq` shows `messages_ready >= 1`. `docker compose logs ms-membresias` shows one `"Pago procesado..."` line and one `"ALERTA: pago rechazado..."` line.

- [ ] **Step 10: Run the full Postman collection with newman**

Run: `newman run postman/Gimnasio-Microservicios.postman_collection.json`
Expected: all requests pass — this is the first run that exercises every flow end to end.

Run: `docker compose down`

- [ ] **Step 11: Commit**

```bash
git add ms-membresias/src/main/java/co/analisys/membresias/messaging \
  ms-membresias/src/main/java/co/analisys/membresias/dto/PagoRequest.java \
  ms-membresias/src/main/java/co/analisys/membresias/config/RabbitMQConfig.java \
  ms-membresias/src/main/java/co/analisys/membresias/service/MiembroService.java \
  ms-membresias/src/main/java/co/analisys/membresias/controller/MiembroController.java \
  postman/Gimnasio-Microservicios.postman_collection.json
git commit -m "Add payment Dead Letter Queue in ms-membresias"
```

---

### Task 5: Documentación (README)

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: nothing new (documents Tasks 1-4's finished state).

- [ ] **Step 1: Add a RabbitMQ section to the README**

Modify `README.md`. Replace:

```markdown
## Documentación de la API (Swagger/OpenAPI)
```

with:

```markdown
## Mensajería asíncrona: RabbitMQ

Tres flujos de mensajería sobre RabbitMQ (diseño completo en
[`docs/superpowers/specs/2026-09-14-rabbitmq-integracion-design.md`](docs/superpowers/specs/2026-09-14-rabbitmq-integracion-design.md)):

| Flujo | Productor → Consumidor | Exchange / cola |
|---|---|---|
| Notificación de inscripción | `ms-membresias` → `ms-membresias` | `membresias.exchange` → `notificacion.inscripcion.queue` |
| Pub/sub cambio de horario | `ms-programacion` → `ms-personal` | `programacion.exchange` → `horario.clase.queue` |
| DLQ de pagos fallidos | `ms-membresias` → `ms-membresias` | `pagos.exchange` → `pagos.procesar.queue` (dead-letters a `pagos.dlx` → `pagos.dlq` si el monto es inválido) |

`docker compose up` levanta también el contenedor `rabbitmq` (imagen `rabbitmq:3.13-management`). Consola de gestión: `http://localhost:15672` — `guest` / `guest`. Desde ahí se puede inspeccionar cada cola (mensajes pendientes, tasa de entrega) mientras se prueban los endpoints que publican eventos: `POST /api/miembros`, `PATCH /api/clases/{id}/horario`, `POST /api/miembros/{id}/pagos`.

## Documentación de la API (Swagger/OpenAPI)
```

- [ ] **Step 2: Mention the new Postman folder**

Modify `README.md`. Replace:

```markdown
O importa [`postman/Gimnasio-Microservicios.postman_collection.json`](postman/Gimnasio-Microservicios.postman_collection.json) en Postman — trae una carpeta por microservicio con casos válidos y casos que verifican las invariantes de dominio (email inválido/duplicado, capacidad y cantidad negativas, especialidad fuera de catálogo, etc.). También se puede correr desde la terminal con [newman](https://github.com/postmanlabs/newman):
```

with:

```markdown
O importa [`postman/Gimnasio-Microservicios.postman_collection.json`](postman/Gimnasio-Microservicios.postman_collection.json) en Postman — trae una carpeta por microservicio con casos válidos y casos que verifican las invariantes de dominio (email inválido/duplicado, capacidad y cantidad negativas, especialidad fuera de catálogo, etc.), más `1. Seguridad (JWT)` (casos 401/403) y `2. RabbitMQ` (dispara cada flujo de mensajería y verifica contra la Management API de RabbitMQ que el mensaje pasó por la cola esperada, incluyendo el camino que cae a la DLQ de pagos). También se puede correr desde la terminal con [newman](https://github.com/postmanlabs/newman):
```

- [ ] **Step 3: Review rendered output**

Run: read `README.md` and confirm both edits render correctly and no other section references are broken (table formatting, links).

- [ ] **Step 4: Commit**

```bash
git add README.md
git commit -m "Document the RabbitMQ integration in the README"
```

---

## Self-Review Notes

- **Spec coverage:** Infra (spec "Infraestructura") → Task 1. Flujo 1 → Task 2. Flujo 2 (incl. the shared-package DTO decision) → Task 3. Flujo 3/DLQ → Task 4. Postman/newman (JWT + RabbitMQ folders) → spread across Tasks 2-4, matching the endpoint each flow introduces. README → Task 5. No spec section is without a task.
- **Placeholder scan:** no TBD/TODO; every step has literal code or literal JSON to write, not a description of it.
- **Type consistency:** `RabbitMQConfig.MEMBRESIAS_EXCHANGE` / `.MIEMBRO_INSCRITO_ROUTING_KEY` (Task 2) are reused verbatim in Task 2's `MiembroService`; `RabbitMQConfig.PAGOS_*` and `PAGO_*_ROUTING_KEY` constants (Task 4) match between the config class and `MiembroService.registrarPago`/the two listeners. `HorarioClaseCambiadoEvento`'s field names (`claseId`, `nombreClase`, `horarioAnterior`, `horarioNuevo`, `entrenadorId`) match between the two duplicated files (Task 3, Steps 1-2) and their one producer (`ClaseService.cambiarHorario`) and one consumer (`HorarioClaseListener.onHorarioCambiado`). `ClaseService.cambiarHorario(ClaseId, LocalDateTime)` return type `Clase` matches what `ClaseController.cambiarHorario` returns directly.

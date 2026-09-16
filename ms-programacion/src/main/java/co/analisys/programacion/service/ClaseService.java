package co.analisys.programacion.service;

import co.analisys.gimnasio.eventos.HorarioClaseCambiadoEvento;
import co.analisys.programacion.client.PersonalClient;
import co.analisys.programacion.config.KafkaProducerConfig;
import co.analisys.programacion.config.RabbitMQConfig;
import co.analisys.programacion.dto.ClaseDetalleDTO;
import co.analisys.programacion.dto.ClaseRequest;
import co.analisys.programacion.dto.EntrenadorDTO;
import co.analisys.programacion.messaging.dto.OcupacionClaseEvento;
import co.analisys.programacion.model.Capacidad;
import co.analisys.programacion.model.Clase;
import co.analisys.programacion.model.ClaseId;
import co.analisys.programacion.model.EntrenadorId;
import co.analisys.programacion.repository.ClaseRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
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
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

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

    public void reportarOcupacion(ClaseId id, int ocupacionActual) {
        claseRepository.findById(id.valor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clase no encontrada: " + id.valor()));
        if (ocupacionActual < 0) {
            throw new IllegalArgumentException("La ocupacion actual no puede ser negativa");
        }
        OcupacionClaseEvento evento = new OcupacionClaseEvento(id.valor(), ocupacionActual, Instant.now());
        kafkaTemplate.send(KafkaProducerConfig.OCUPACION_CLASES_TOPIC, id.valor().toString(), evento);
    }
}

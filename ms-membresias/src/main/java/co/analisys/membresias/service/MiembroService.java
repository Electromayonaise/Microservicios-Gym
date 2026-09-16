package co.analisys.membresias.service;

import co.analisys.membresias.config.KafkaProducerConfig;
import co.analisys.membresias.config.RabbitMQConfig;
import co.analisys.membresias.messaging.dto.DatoEntrenamientoEvento;
import co.analisys.membresias.messaging.dto.InscripcionNotificacionDTO;
import co.analisys.membresias.messaging.dto.PagoDTO;
import co.analisys.membresias.model.Email;
import co.analisys.membresias.model.Miembro;
import co.analisys.membresias.repository.MiembroRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class MiembroService {
    @Autowired
    private MiembroRepository miembroRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

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

    public void registrarPago(Long miembroId, BigDecimal monto, String concepto) {
        if (!miembroRepository.existsById(miembroId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro no encontrado: " + miembroId);
        }
        PagoDTO pago = new PagoDTO(miembroId, monto, concepto);
        rabbitTemplate.convertAndSend(RabbitMQConfig.PAGOS_EXCHANGE, RabbitMQConfig.PAGO_PROCESAR_ROUTING_KEY, pago);
    }

    public void registrarEntrenamiento(Long miembroId, int duracionMinutos, int calorias) {
        if (!miembroRepository.existsById(miembroId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro no encontrado: " + miembroId);
        }
        if (duracionMinutos <= 0 || calorias <= 0) {
            throw new IllegalArgumentException("La duracion y las calorias deben ser mayores a cero");
        }
        DatoEntrenamientoEvento dato = new DatoEntrenamientoEvento(miembroId, duracionMinutos, calorias, Instant.now());
        kafkaTemplate.send(KafkaProducerConfig.DATOS_ENTRENAMIENTO_TOPIC, miembroId.toString(), dato);
    }
}

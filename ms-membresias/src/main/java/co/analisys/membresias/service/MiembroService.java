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

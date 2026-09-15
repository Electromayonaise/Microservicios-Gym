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

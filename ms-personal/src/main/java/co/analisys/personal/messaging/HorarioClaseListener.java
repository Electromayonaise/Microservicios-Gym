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

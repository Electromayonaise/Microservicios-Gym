package co.analisys.programacion.messaging;

import co.analisys.programacion.messaging.dto.OcupacionClaseEvento;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OcupacionClaseListener extends AbstractConsumerSeekAware {

    public static final String LISTENER_ID = "ocupacion-clases-listener";
    public static final String GROUP_ID = "ms-programacion-dashboard";

    @KafkaListener(id = LISTENER_ID, topics = "ocupacion-clases", groupId = GROUP_ID)
    public void onOcupacionActualizada(OcupacionClaseEvento evento, Acknowledgment acknowledgment) {
        System.out.println("Dashboard: clase " + evento.claseId() + " tiene ahora " + evento.ocupacionActual()
                + " personas (timestamp=" + evento.timestamp() + ")");
        acknowledgment.acknowledge();
    }
}

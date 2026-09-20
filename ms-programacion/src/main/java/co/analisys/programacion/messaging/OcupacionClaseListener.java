package co.analisys.programacion.messaging;

import co.analisys.programacion.messaging.dto.OcupacionClaseEvento;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.AbstractConsumerSeekAware;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class OcupacionClaseListener extends AbstractConsumerSeekAware {

    public static final String LISTENER_ID = "ocupacion-clases-listener";
    public static final String GROUP_ID = "ms-programacion-dashboard";

    private final AtomicLong mensajesProcesados = new AtomicLong();

    @KafkaListener(id = LISTENER_ID, topics = "ocupacion-clases", groupId = GROUP_ID)
    public void onOcupacionActualizada(OcupacionClaseEvento evento, Acknowledgment acknowledgment) {
        System.out.println("Dashboard: clase " + evento.claseId() + " tiene ahora " + evento.ocupacionActual()
                + " personas (timestamp=" + evento.timestamp() + ")");
        mensajesProcesados.incrementAndGet();
        acknowledgment.acknowledge();
    }

    public long getMensajesProcesados() {
        return mensajesProcesados.get();
    }
}

package co.analisys.membresias.messaging;

import co.analisys.membresias.messaging.dto.ResumenEntrenamientoEvento;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class ResumenEntrenamientoListener {

    @KafkaListener(topics = "entrenamiento-resumen", groupId = "ms-membresias-resumen")
    public void onResumenGenerado(ResumenEntrenamientoEvento resumen, Acknowledgment acknowledgment) {
        System.out.println("Resumen de entrenamiento miembroId=" + resumen.miembroId()
                + ": " + resumen.cantidadSesiones() + " sesiones, " + resumen.totalDuracionMinutos()
                + " min, " + resumen.totalCalorias() + " calorias (ventana " + resumen.ventanaInicio()
                + " - " + resumen.ventanaFin() + ")");
        acknowledgment.acknowledge();
    }
}

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

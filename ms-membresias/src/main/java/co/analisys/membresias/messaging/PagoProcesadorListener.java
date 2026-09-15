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

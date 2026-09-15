package co.analisys.membresias.messaging.dto;

import java.math.BigDecimal;

public record PagoDTO(Long miembroId, BigDecimal monto, String concepto) {
}

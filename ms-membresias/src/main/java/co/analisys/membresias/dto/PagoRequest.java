package co.analisys.membresias.dto;

import java.math.BigDecimal;

public record PagoRequest(BigDecimal monto, String concepto) {
}

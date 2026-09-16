package co.analisys.programacion.messaging.dto;

import java.time.Instant;

public record OcupacionClaseEvento(Long claseId, int ocupacionActual, Instant timestamp) {
}

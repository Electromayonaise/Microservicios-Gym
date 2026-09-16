package co.analisys.membresias.messaging.dto;

import java.time.Instant;

public record DatoEntrenamientoEvento(Long miembroId, int duracionMinutos, int calorias, Instant timestamp) {
}

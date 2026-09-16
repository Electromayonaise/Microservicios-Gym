package co.analisys.membresias.messaging.dto;

import java.time.Instant;

public record ResumenEntrenamientoEvento(Long miembroId, int totalDuracionMinutos, int totalCalorias,
                                          int cantidadSesiones, Instant ventanaInicio, Instant ventanaFin) {
}

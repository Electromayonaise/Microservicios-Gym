package co.analisys.membresias.messaging.dto;

import java.time.LocalDate;

public record InscripcionNotificacionDTO(Long miembroId, String nombre, String email, LocalDate fechaInscripcion) {
}

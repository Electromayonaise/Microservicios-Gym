package co.analisys.programacion.dto;

import java.time.LocalDateTime;

public record CambioHorarioRequest(LocalDateTime nuevoHorario) {
}

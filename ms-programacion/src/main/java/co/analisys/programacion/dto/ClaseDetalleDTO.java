package co.analisys.programacion.dto;

import java.time.LocalDateTime;

public record ClaseDetalleDTO(
        Long id,
        String nombre,
        LocalDateTime horario,
        int capacidadMaxima,
        EntrenadorDTO entrenador
) {
}

package co.analisys.programacion.dto;

import java.time.LocalDateTime;

public record ClaseRequest(String nombre, LocalDateTime horario, int capacidadMaxima, Long entrenadorId) {
}

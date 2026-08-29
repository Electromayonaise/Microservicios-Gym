package co.analisys.programacion.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Raíz de agregado del contexto Programación. La invariante "capacidadMaxima > 0"
 * la protege el value object {@link Capacidad}; entrenadorId no puede ser nulo
 * (se valida contra el contexto Personal en el servicio antes de construir la clase)
 * y está tipado como {@link EntrenadorId} para no confundirlo con {@link ClaseId}.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Clase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private LocalDateTime horario;
    @Embedded
    private Capacidad capacidadMaxima;

    /**
     * Referencia por id al agregado Entrenador del contexto Personal (ms-personal).
     * Sustituye el @ManyToOne del monolito para no compartir base de datos entre contextos.
     */
    @Embedded
    private EntrenadorId entrenadorId;

    private Clase(String nombre, LocalDateTime horario, Capacidad capacidadMaxima, EntrenadorId entrenadorId) {
        this.nombre = nombre;
        this.horario = horario;
        this.capacidadMaxima = capacidadMaxima;
        this.entrenadorId = entrenadorId;
    }

    public static Clase programar(String nombre, LocalDateTime horario, Capacidad capacidadMaxima, EntrenadorId entrenadorId) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de la clase no puede estar vacío");
        }
        if (horario == null) {
            throw new IllegalArgumentException("La clase debe tener un horario");
        }
        if (entrenadorId == null) {
            throw new IllegalArgumentException("La clase debe asignarse a un entrenador (entrenadorId)");
        }
        return new Clase(nombre, horario, capacidadMaxima, entrenadorId);
    }
}

package co.analisys.programacion.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Clase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private LocalDateTime horario;
    private int capacidadMaxima;

    /**
     * Referencia por id al agregado Entrenador del contexto Personal (ms-personal).
     * Sustituye el @ManyToOne del monolito para no compartir base de datos entre contextos.
     */
    private Long entrenadorId;
}

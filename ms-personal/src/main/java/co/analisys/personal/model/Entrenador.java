package co.analisys.personal.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Raíz de agregado del contexto Personal. Protege que el nombre no quede vacío
 * y que la especialidad sea siempre una del catálogo cerrado ({@link Especialidad}).
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Entrenador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    @Enumerated(EnumType.STRING)
    private Especialidad especialidad;

    private Entrenador(String nombre, Especialidad especialidad) {
        this.nombre = nombre;
        this.especialidad = especialidad;
    }

    public static Entrenador registrar(String nombre, Especialidad especialidad) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del entrenador no puede estar vacío");
        }
        if (especialidad == null) {
            throw new IllegalArgumentException("El entrenador debe tener una especialidad");
        }
        return new Entrenador(nombre, especialidad);
    }
}

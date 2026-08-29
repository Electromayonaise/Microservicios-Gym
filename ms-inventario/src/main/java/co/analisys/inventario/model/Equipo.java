package co.analisys.inventario.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Raíz de agregado del contexto Inventario. La invariante "cantidad >= 0"
 * la protege el value object {@link Cantidad}, no el llamador.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String descripcion;
    @Embedded
    private Cantidad cantidad;

    private Equipo(String nombre, String descripcion, Cantidad cantidad) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
    }

    public static Equipo registrar(String nombre, String descripcion, Cantidad cantidad) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del equipo no puede estar vacío");
        }
        return new Equipo(nombre, descripcion, cantidad);
    }
}

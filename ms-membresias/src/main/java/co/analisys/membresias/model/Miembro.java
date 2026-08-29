package co.analisys.membresias.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Raíz de agregado del contexto Membresías. Protege sus propias invariantes:
 * el email debe ser válido (ver {@link Email}) y la fecha de inscripción
 * siempre se fija a "hoy" en el momento del registro, nunca la decide el llamador.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Miembro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    @Embedded
    private Email email;
    private LocalDate fechaInscripcion;

    private Miembro(String nombre, Email email) {
        this.nombre = nombre;
        this.email = email;
        this.fechaInscripcion = LocalDate.now();
    }

    public static Miembro registrar(String nombre, Email email) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del miembro no puede estar vacío");
        }
        return new Miembro(nombre, email);
    }
}

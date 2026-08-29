package co.analisys.programacion.model;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/**
 * Identifica a un Entrenador del contexto Personal (ms-personal) dentro de
 * ms-programacion. Existe para no confundir por tipo un id de Entrenador
 * con un id de Clase ({@link ClaseId}) — es el único cruce entre agregados
 * de distinto contexto en todo el sistema.
 */
@Embeddable
public class EntrenadorId {

    @Column(name = "entrenador_id", nullable = false)
    private Long valor;

    protected EntrenadorId() {
    }

    public EntrenadorId(Long valor) {
        if (valor == null) {
            throw new IllegalArgumentException("El id del entrenador no puede ser nulo");
        }
        this.valor = valor;
    }

    @JsonValue
    public Long getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntrenadorId that)) return false;
        return Objects.equals(valor, that.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return String.valueOf(valor);
    }
}

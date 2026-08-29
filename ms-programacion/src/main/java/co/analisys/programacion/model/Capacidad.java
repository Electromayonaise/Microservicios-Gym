package co.analisys.programacion.model;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class Capacidad {

    @Column(name = "capacidad_maxima", nullable = false)
    private int valor;

    protected Capacidad() {
    }

    public Capacidad(int valor) {
        if (valor <= 0) {
            throw new IllegalArgumentException("La capacidad máxima de una clase debe ser mayor que 0");
        }
        this.valor = valor;
    }

    @JsonValue
    public int getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Capacidad that)) return false;
        return valor == that.valor;
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

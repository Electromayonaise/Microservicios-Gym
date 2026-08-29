package co.analisys.inventario.model;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

@Embeddable
public class Cantidad {

    @Column(name = "cantidad", nullable = false)
    private int valor;

    protected Cantidad() {
    }

    public Cantidad(int valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("La cantidad de un equipo no puede ser negativa");
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
        if (!(o instanceof Cantidad that)) return false;
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

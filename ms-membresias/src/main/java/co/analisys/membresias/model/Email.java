package co.analisys.membresias.model;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public class Email {

    private static final Pattern FORMATO = Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    @Column(name = "email", nullable = false)
    private String valor;

    protected Email() {
    }

    public Email(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        String normalizado = valor.trim().toLowerCase();
        if (!FORMATO.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("Email con formato inválido: " + valor);
        }
        this.valor = normalizado;
    }

    @JsonValue
    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email email)) return false;
        return Objects.equals(valor, email.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}

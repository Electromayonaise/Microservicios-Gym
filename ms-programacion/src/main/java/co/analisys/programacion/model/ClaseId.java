package co.analisys.programacion.model;

/**
 * Identifica a una Clase dentro de ms-programacion. No está mapeado por JPA
 * (el {@code @Id} interno de {@link Clase} sigue siendo un {@code Long}
 * autogenerado por Hibernate); se usa en la capa de aplicación (servicio,
 * controlador) para no aceptar por error un id de otro tipo — en particular
 * un {@link EntrenadorId} — donde se espera el id de una Clase.
 */
public record ClaseId(Long valor) {
    public ClaseId {
        if (valor == null) {
            throw new IllegalArgumentException("El id de la clase no puede ser nulo");
        }
    }
}

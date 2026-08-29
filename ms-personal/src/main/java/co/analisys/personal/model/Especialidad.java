package co.analisys.personal.model;

/**
 * Catálogo cerrado de especialidades que puede dictar un entrenador del gimnasio.
 * Se modela como enum (no como Value Object de texto libre) porque el conjunto
 * de valores válidos es conocido y acotado de antemano.
 */
public enum Especialidad {
    YOGA,
    SPINNING,
    CROSSFIT,
    PILATES,
    MUSCULACION,
    NATACION,
    BOXEO,
    ZUMBA,
    FUNCIONAL,
    CALISTENIA
}

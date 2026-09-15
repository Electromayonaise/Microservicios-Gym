package co.analisys.gimnasio.eventos;

import java.time.LocalDateTime;

/**
 * Evento de mensajeria compartido entre ms-programacion (productor) y
 * ms-personal (consumidor) para el cambio de horario de una clase. Vive en
 * el mismo paquete literal en ambos microservicios (no en el paquete base
 * de ninguno) para que Jackson2JsonMessageConverter resuelva el tipo por
 * el header __TypeId__ sin configuracion adicional -- ver la seccion
 * "Flujo 2" de docs/superpowers/specs/2026-09-14-rabbitmq-integracion-design.md.
 */
public record HorarioClaseCambiadoEvento(Long claseId, String nombreClase, LocalDateTime horarioAnterior,
                                          LocalDateTime horarioNuevo, Long entrenadorId) {
}

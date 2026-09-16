package co.analisys.membresias.controller;

import co.analisys.membresias.dto.EntrenamientoRequest;
import co.analisys.membresias.dto.MiembroRequest;
import co.analisys.membresias.dto.PagoRequest;
import co.analisys.membresias.model.Miembro;
import co.analisys.membresias.service.MiembroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Membresías", description = "Registro y consulta de miembros del gimnasio")
@RestController
@RequestMapping("/api/miembros")
public class MiembroController {
    @Autowired
    private MiembroService miembroService;

    @Operation(
        summary = "Registrar un miembro",
        description = "Registra un nuevo miembro del gimnasio a partir de su nombre y correo electrónico.")
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Miembro registrarMiembro(@RequestBody MiembroRequest request) {
        return miembroService.registrarMiembro(request.nombre(), request.email());
    }

    @Operation(
        summary = "Consultar todos los miembros",
        description = "Obtiene la lista de todos los miembros registrados en el gimnasio.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER')")
    public List<Miembro> obtenerTodosMiembros() {
        return miembroService.obtenerTodosMiembros();
    }

    @Operation(
        summary = "Registrar un pago",
        description = "Encola el pago de un miembro para procesamiento asincrono via RabbitMQ. Un monto invalido cae a la Dead Letter Queue de pagos.")
    @PostMapping("/{id}/pagos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MEMBER')")
    public void registrarPago(
            @Parameter(description = "Identificador del miembro") @PathVariable Long id,
            @RequestBody PagoRequest request) {
        miembroService.registrarPago(id, request.monto(), request.concepto());
    }

    @Operation(
        summary = "Registrar una sesion de entrenamiento",
        description = "Publica un dato de entrenamiento (topic datos-entrenamiento) que Kafka Streams agrega en ventanas de 5 minutos y resume en el topic entrenamiento-resumen.")
    @PostMapping("/{id}/entrenamientos")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MEMBER')")
    public void registrarEntrenamiento(
            @Parameter(description = "Identificador del miembro") @PathVariable Long id,
            @RequestBody EntrenamientoRequest request) {
        miembroService.registrarEntrenamiento(id, request.duracionMinutos(), request.calorias());
    }
}

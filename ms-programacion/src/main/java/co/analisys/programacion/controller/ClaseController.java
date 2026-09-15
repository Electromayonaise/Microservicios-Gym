package co.analisys.programacion.controller;

import co.analisys.programacion.dto.CambioHorarioRequest;
import co.analisys.programacion.dto.ClaseDetalleDTO;
import co.analisys.programacion.dto.ClaseRequest;
import co.analisys.programacion.model.Clase;
import co.analisys.programacion.model.ClaseId;
import co.analisys.programacion.service.ClaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Programación", description = "Programación de clases del gimnasio")
@RestController
@RequestMapping("/api/clases")
public class ClaseController {
    @Autowired
    private ClaseService claseService;

    @Operation(
        summary = "Programar una clase",
        description = "Programa una nueva clase asignada a un entrenador existente (valida el entrenadorId contra ms-personal).")
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER')")
    public Clase programarClase(@RequestBody ClaseRequest request) {
        return claseService.programarClase(request);
    }

    @Operation(
        summary = "Consultar todas las clases",
        description = "Obtiene la lista de todas las clases programadas.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER', 'ROLE_MEMBER')")
    public List<Clase> obtenerTodasClases() {
        return claseService.obtenerTodasClases();
    }

    @Operation(
        summary = "Consultar una clase con su entrenador",
        description = "Obtiene el detalle de una clase junto con los datos de su entrenador asignado (consulta a ms-personal).")
    @GetMapping("/{id}/entrenador")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER', 'ROLE_MEMBER')")
    public ClaseDetalleDTO obtenerClaseConEntrenador(
            @Parameter(description = "Identificador de la clase") @PathVariable Long id) {
        return claseService.obtenerClaseConEntrenador(new ClaseId(id));
    }

    @Operation(
        summary = "Cambiar el horario de una clase",
        description = "Reprograma una clase existente y publica un evento pub/sub (clase.horario.cambiado) para que ms-personal notifique al entrenador asignado.")
    @PatchMapping("/{id}/horario")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER')")
    public Clase cambiarHorario(
            @Parameter(description = "Identificador de la clase") @PathVariable Long id,
            @RequestBody CambioHorarioRequest request) {
        return claseService.cambiarHorario(new ClaseId(id), request.nuevoHorario());
    }
}

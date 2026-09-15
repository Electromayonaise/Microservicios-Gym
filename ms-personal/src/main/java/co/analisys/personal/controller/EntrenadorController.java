package co.analisys.personal.controller;

import co.analisys.personal.dto.EntrenadorRequest;
import co.analisys.personal.model.Entrenador;
import co.analisys.personal.service.EntrenadorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Personal", description = "Gestión de entrenadores del gimnasio")
@RestController
@RequestMapping("/api/entrenadores")
public class EntrenadorController {
    @Autowired
    private EntrenadorService entrenadorService;

    @Operation(
        summary = "Agregar un entrenador",
        description = "Registra un nuevo entrenador con su nombre y especialidad.")
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Entrenador agregarEntrenador(@RequestBody EntrenadorRequest request) {
        return entrenadorService.agregarEntrenador(request);
    }

    @Operation(
        summary = "Consultar todos los entrenadores",
        description = "Obtiene la lista de todos los entrenadores registrados.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER', 'ROLE_MEMBER')")
    public List<Entrenador> obtenerTodosEntrenadores() {
        return entrenadorService.obtenerTodosEntrenadores();
    }

    @Operation(
        summary = "Consultar un entrenador por id",
        description = "Obtiene los datos de un entrenador a partir de su identificador.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER', 'ROLE_MEMBER')")
    public Entrenador obtenerEntrenadorPorId(
            @Parameter(description = "Identificador del entrenador") @PathVariable Long id) {
        return entrenadorService.obtenerEntrenadorPorId(id);
    }
}

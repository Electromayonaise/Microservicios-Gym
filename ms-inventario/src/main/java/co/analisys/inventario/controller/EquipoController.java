package co.analisys.inventario.controller;

import co.analisys.inventario.dto.EquipoRequest;
import co.analisys.inventario.model.Equipo;
import co.analisys.inventario.service.EquipoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Inventario", description = "Gestión del inventario de equipos del gimnasio")
@RestController
@RequestMapping("/api/equipos")
public class EquipoController {
    @Autowired
    private EquipoService equipoService;

    @Operation(
        summary = "Agregar un equipo",
        description = "Registra un nuevo equipo en el inventario con su nombre, descripción y cantidad.")
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Equipo agregarEquipo(@RequestBody EquipoRequest request) {
        return equipoService.agregarEquipo(request);
    }

    @Operation(
        summary = "Consultar todos los equipos",
        description = "Obtiene la lista de todos los equipos registrados en el inventario.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TRAINER', 'ROLE_MEMBER')")
    public List<Equipo> obtenerTodosEquipos() {
        return equipoService.obtenerTodosEquipos();
    }
}

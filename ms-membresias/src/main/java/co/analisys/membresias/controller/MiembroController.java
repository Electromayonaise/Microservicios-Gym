package co.analisys.membresias.controller;

import co.analisys.membresias.dto.MiembroRequest;
import co.analisys.membresias.model.Miembro;
import co.analisys.membresias.service.MiembroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
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
}

package co.analisys.membresias.controller;

import co.analisys.membresias.dto.MiembroRequest;
import co.analisys.membresias.model.Miembro;
import co.analisys.membresias.service.MiembroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/miembros")
public class MiembroController {
    @Autowired
    private MiembroService miembroService;

    @PostMapping
    public Miembro registrarMiembro(@RequestBody MiembroRequest request) {
        return miembroService.registrarMiembro(request.nombre(), request.email());
    }

    @GetMapping
    public List<Miembro> obtenerTodosMiembros() {
        return miembroService.obtenerTodosMiembros();
    }
}

package co.analisys.programacion.controller;

import co.analisys.programacion.dto.ClaseDetalleDTO;
import co.analisys.programacion.model.Clase;
import co.analisys.programacion.service.ClaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clases")
public class ClaseController {
    @Autowired
    private ClaseService claseService;

    @PostMapping
    public Clase programarClase(@RequestBody Clase clase) {
        return claseService.programarClase(clase);
    }

    @GetMapping
    public List<Clase> obtenerTodasClases() {
        return claseService.obtenerTodasClases();
    }

    @GetMapping("/{id}/entrenador")
    public ClaseDetalleDTO obtenerClaseConEntrenador(@PathVariable Long id) {
        return claseService.obtenerClaseConEntrenador(id);
    }
}

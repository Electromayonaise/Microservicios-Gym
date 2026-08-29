package co.analisys.personal.controller;

import co.analisys.personal.dto.EntrenadorRequest;
import co.analisys.personal.model.Entrenador;
import co.analisys.personal.service.EntrenadorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entrenadores")
public class EntrenadorController {
    @Autowired
    private EntrenadorService entrenadorService;

    @PostMapping
    public Entrenador agregarEntrenador(@RequestBody EntrenadorRequest request) {
        return entrenadorService.agregarEntrenador(request);
    }

    @GetMapping
    public List<Entrenador> obtenerTodosEntrenadores() {
        return entrenadorService.obtenerTodosEntrenadores();
    }

    @GetMapping("/{id}")
    public Entrenador obtenerEntrenadorPorId(@PathVariable Long id) {
        return entrenadorService.obtenerEntrenadorPorId(id);
    }
}

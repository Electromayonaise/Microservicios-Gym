package co.analisys.inventario.controller;

import co.analisys.inventario.model.Equipo;
import co.analisys.inventario.service.EquipoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {
    @Autowired
    private EquipoService equipoService;

    @PostMapping
    public Equipo agregarEquipo(@RequestBody Equipo equipo) {
        return equipoService.agregarEquipo(equipo);
    }

    @GetMapping
    public List<Equipo> obtenerTodosEquipos() {
        return equipoService.obtenerTodosEquipos();
    }
}

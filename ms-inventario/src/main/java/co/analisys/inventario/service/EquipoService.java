package co.analisys.inventario.service;

import co.analisys.inventario.dto.EquipoRequest;
import co.analisys.inventario.model.Cantidad;
import co.analisys.inventario.model.Equipo;
import co.analisys.inventario.repository.EquipoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipoService {
    @Autowired
    private EquipoRepository equipoRepository;

    public Equipo agregarEquipo(EquipoRequest request) {
        Equipo equipo = Equipo.registrar(request.nombre(), request.descripcion(), new Cantidad(request.cantidad()));
        return equipoRepository.save(equipo);
    }

    public List<Equipo> obtenerTodosEquipos() {
        return equipoRepository.findAll();
    }
}

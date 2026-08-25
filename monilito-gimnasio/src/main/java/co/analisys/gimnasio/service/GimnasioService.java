package co.analisys.gimnasio.service;

import co.analisys.gimnasio.model.Clase;
import co.analisys.gimnasio.model.Entrenador;
import co.analisys.gimnasio.model.Equipo;
import co.analisys.gimnasio.model.Miembro;
import co.analisys.gimnasio.repository.ClaseRepository;
import co.analisys.gimnasio.repository.EntrenadorRepository;
import co.analisys.gimnasio.repository.EquipoRepository;
import co.analisys.gimnasio.repository.MiembroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GimnasioService {
    @Autowired
    private MiembroRepository miembroRepository;
    @Autowired
    private ClaseRepository claseRepository;
    @Autowired
    private EntrenadorRepository entrenadorRepository;
    @Autowired
    private EquipoRepository equipoRepository;

    public Miembro registrarMiembro(Miembro miembro) {
        return miembroRepository.save(miembro);
    }

    public Clase programarClase(Clase clase) {
        return claseRepository.save(clase);
    }

    public Entrenador agregarEntrenador(Entrenador entrenador) {
        return entrenadorRepository.save(entrenador);
    }

    public Equipo agregarEquipo(Equipo equipo) {
        return equipoRepository.save(equipo);
    }

    public List<Miembro> obtenerTodosMiembros() {
        return miembroRepository.findAll();
    }

    public List<Clase> obtenerTodasClases() {
        return claseRepository.findAll();
    }

    public List<Entrenador> obtenerTodosEntrenadores() {
        return entrenadorRepository.findAll();
    }

    public List<Equipo> obtenerTodosEquipos() {
        return equipoRepository.findAll();
    }
}

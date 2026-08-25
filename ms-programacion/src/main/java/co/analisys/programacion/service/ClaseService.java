package co.analisys.programacion.service;

import co.analisys.programacion.client.PersonalClient;
import co.analisys.programacion.dto.ClaseDetalleDTO;
import co.analisys.programacion.dto.EntrenadorDTO;
import co.analisys.programacion.model.Clase;
import co.analisys.programacion.repository.ClaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ClaseService {
    @Autowired
    private ClaseRepository claseRepository;
    @Autowired
    private PersonalClient personalClient;

    public Clase programarClase(Clase clase) {
        personalClient.obtenerEntrenador(clase.getEntrenadorId());
        return claseRepository.save(clase);
    }

    public List<Clase> obtenerTodasClases() {
        return claseRepository.findAll();
    }

    public ClaseDetalleDTO obtenerClaseConEntrenador(Long id) {
        Clase clase = claseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clase no encontrada: " + id));
        EntrenadorDTO entrenador = personalClient.obtenerEntrenador(clase.getEntrenadorId());
        return new ClaseDetalleDTO(clase.getId(), clase.getNombre(), clase.getHorario(), clase.getCapacidadMaxima(), entrenador);
    }
}

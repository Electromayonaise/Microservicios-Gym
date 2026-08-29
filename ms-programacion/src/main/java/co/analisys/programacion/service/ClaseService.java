package co.analisys.programacion.service;

import co.analisys.programacion.client.PersonalClient;
import co.analisys.programacion.dto.ClaseDetalleDTO;
import co.analisys.programacion.dto.ClaseRequest;
import co.analisys.programacion.dto.EntrenadorDTO;
import co.analisys.programacion.model.Capacidad;
import co.analisys.programacion.model.Clase;
import co.analisys.programacion.model.ClaseId;
import co.analisys.programacion.model.EntrenadorId;
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

    public Clase programarClase(ClaseRequest request) {
        if (request.entrenadorId() == null) {
            throw new IllegalArgumentException("La clase debe asignarse a un entrenador (entrenadorId)");
        }
        EntrenadorId entrenadorId = new EntrenadorId(request.entrenadorId());
        personalClient.obtenerEntrenador(entrenadorId);
        Clase clase = Clase.programar(request.nombre(), request.horario(),
                new Capacidad(request.capacidadMaxima()), entrenadorId);
        return claseRepository.save(clase);
    }

    public List<Clase> obtenerTodasClases() {
        return claseRepository.findAll();
    }

    public ClaseDetalleDTO obtenerClaseConEntrenador(ClaseId id) {
        Clase clase = claseRepository.findById(id.valor())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Clase no encontrada: " + id.valor()));
        EntrenadorDTO entrenador = personalClient.obtenerEntrenador(clase.getEntrenadorId());
        return new ClaseDetalleDTO(clase.getId(), clase.getNombre(), clase.getHorario(),
                clase.getCapacidadMaxima().getValor(), entrenador);
    }
}

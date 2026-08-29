package co.analisys.personal.service;

import co.analisys.personal.dto.EntrenadorRequest;
import co.analisys.personal.model.Entrenador;
import co.analisys.personal.model.Especialidad;
import co.analisys.personal.repository.EntrenadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.List;

@Service
public class EntrenadorService {
    @Autowired
    private EntrenadorRepository entrenadorRepository;

    public Entrenador agregarEntrenador(EntrenadorRequest request) {
        Especialidad especialidad = parseEspecialidad(request.especialidad());
        Entrenador entrenador = Entrenador.registrar(request.nombre(), especialidad);
        return entrenadorRepository.save(entrenador);
    }

    private Especialidad parseEspecialidad(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "El entrenador debe tener una especialidad. Valores válidos: " + Arrays.toString(Especialidad.values()));
        }
        try {
            return Especialidad.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Especialidad inválida: " + valor + ". Valores válidos: " + Arrays.toString(Especialidad.values()));
        }
    }

    public List<Entrenador> obtenerTodosEntrenadores() {
        return entrenadorRepository.findAll();
    }

    public Entrenador obtenerEntrenadorPorId(Long id) {
        return entrenadorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrenador no encontrado: " + id));
    }
}

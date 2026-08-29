package co.analisys.membresias.service;

import co.analisys.membresias.model.Email;
import co.analisys.membresias.model.Miembro;
import co.analisys.membresias.repository.MiembroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MiembroService {
    @Autowired
    private MiembroRepository miembroRepository;

    public Miembro registrarMiembro(String nombre, String email) {
        Email emailValidado = new Email(email);
        if (miembroRepository.existsByEmail(emailValidado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un miembro registrado con el email " + emailValidado.getValor());
        }
        Miembro miembro = Miembro.registrar(nombre, emailValidado);
        return miembroRepository.save(miembro);
    }

    public List<Miembro> obtenerTodosMiembros() {
        return miembroRepository.findAll();
    }
}

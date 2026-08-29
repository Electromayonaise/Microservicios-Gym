package co.analisys.membresias;

import co.analisys.membresias.model.Email;
import co.analisys.membresias.model.Miembro;
import co.analisys.membresias.repository.MiembroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private MiembroRepository miembroRepository;

    @Override
    public void run(String... args) throws Exception {
        // fechaInscripcion ya no se asigna manualmente: la fija Miembro.registrar() = hoy
        Miembro miembro1 = Miembro.registrar("Juan Pérez", new Email("juan@email.com"));
        miembroRepository.save(miembro1);

        Miembro miembro2 = Miembro.registrar("María López", new Email("maria@email.com"));
        miembroRepository.save(miembro2);

        System.out.println("Datos de ejemplo de Membresías cargados exitosamente.");
    }
}

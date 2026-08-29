package co.analisys.personal;

import co.analisys.personal.model.Entrenador;
import co.analisys.personal.model.Especialidad;
import co.analisys.personal.repository.EntrenadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private EntrenadorRepository entrenadorRepository;

    @Override
    public void run(String... args) throws Exception {
        Entrenador entrenador1 = Entrenador.registrar("Carlos Rodríguez", Especialidad.YOGA);
        entrenadorRepository.save(entrenador1);

        Entrenador entrenador2 = Entrenador.registrar("Ana Martínez", Especialidad.SPINNING);
        entrenadorRepository.save(entrenador2);

        System.out.println("Datos de ejemplo de Personal cargados exitosamente.");
    }
}

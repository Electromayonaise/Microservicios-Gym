package co.analisys.personal;

import co.analisys.personal.model.Entrenador;
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
        Entrenador entrenador1 = new Entrenador();
        entrenador1.setNombre("Carlos Rodríguez");
        entrenador1.setEspecialidad("Yoga");
        entrenadorRepository.save(entrenador1);

        Entrenador entrenador2 = new Entrenador();
        entrenador2.setNombre("Ana Martínez");
        entrenador2.setEspecialidad("Spinning");
        entrenadorRepository.save(entrenador2);

        System.out.println("Datos de ejemplo de Personal cargados exitosamente.");
    }
}

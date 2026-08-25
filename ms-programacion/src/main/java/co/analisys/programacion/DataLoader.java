package co.analisys.programacion;

import co.analisys.programacion.model.Clase;
import co.analisys.programacion.repository.ClaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Carga datos de ejemplo guardando directamente en el repositorio (sin pasar por
 * ClaseService/PersonalClient), ya que en el arranque no hay garantia de que
 * ms-personal ya este disponible. Los entrenadorId (1 y 2) coinciden con los
 * ids sembrados por el DataLoader de ms-personal.
 */
@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private ClaseRepository claseRepository;

    @Override
    public void run(String... args) throws Exception {
        Clase clase1 = new Clase();
        clase1.setNombre("Yoga Matutino");
        clase1.setHorario(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0));
        clase1.setCapacidadMaxima(20);
        clase1.setEntrenadorId(1L);
        claseRepository.save(clase1);

        Clase clase2 = new Clase();
        clase2.setNombre("Spinning Vespertino");
        clase2.setHorario(LocalDateTime.now().plusDays(1).withHour(18).withMinute(0));
        clase2.setCapacidadMaxima(15);
        clase2.setEntrenadorId(2L);
        claseRepository.save(clase2);

        System.out.println("Datos de ejemplo de Programación cargados exitosamente.");
    }
}

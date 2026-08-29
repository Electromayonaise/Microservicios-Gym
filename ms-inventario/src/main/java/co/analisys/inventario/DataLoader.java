package co.analisys.inventario;

import co.analisys.inventario.model.Cantidad;
import co.analisys.inventario.model.Equipo;
import co.analisys.inventario.repository.EquipoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private EquipoRepository equipoRepository;

    @Override
    public void run(String... args) throws Exception {
        Equipo equipo1 = Equipo.registrar("Mancuernas", "Set de mancuernas de 5kg", new Cantidad(20));
        equipoRepository.save(equipo1);

        Equipo equipo2 = Equipo.registrar("Bicicleta estática", "Bicicleta para spinning", new Cantidad(15));
        equipoRepository.save(equipo2);

        System.out.println("Datos de ejemplo de Inventario cargados exitosamente.");
    }
}

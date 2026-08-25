package co.analisys.inventario.repository;

import co.analisys.inventario.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {
}

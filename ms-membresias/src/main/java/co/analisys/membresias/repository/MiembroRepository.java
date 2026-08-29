package co.analisys.membresias.repository;

import co.analisys.membresias.model.Email;
import co.analisys.membresias.model.Miembro;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiembroRepository extends JpaRepository<Miembro, Long> {
    boolean existsByEmail(Email email);
}

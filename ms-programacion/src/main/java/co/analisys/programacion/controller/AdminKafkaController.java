package co.analisys.programacion.controller;

import co.analisys.programacion.messaging.OcupacionClaseListener;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Kafka", description = "Operaciones administrativas sobre los consumidores de Kafka")
@RestController
@RequestMapping("/api/admin/kafka")
public class AdminKafkaController {
    @Autowired
    private OcupacionClaseListener ocupacionClaseListener;

    @Operation(
        summary = "Reiniciar el consumo de ocupacion-clases desde el offset 0",
        description = "Mecanismo de recuperacion ante fallos: aprovecha la retencion de 7 dias del topic ocupacion-clases para hacer seek a offset 0 en todas las particiones asignadas al listener del dashboard y reprocesar el historial completo. AbstractConsumerSeekAware.seekToBeginning() es seguro de invocar desde este hilo HTTP: la solicitud de seek se encola y se aplica en el proximo poll del consumidor.")
    @PostMapping("/ocupacion-clases/reiniciar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void reiniciarOcupacionClases() {
        ocupacionClaseListener.seekToBeginning();
    }
}

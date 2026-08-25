package co.analisys.programacion.client;

import co.analisys.programacion.dto.EntrenadorDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cliente REST hacia ms-personal (contexto Personal). Es la unica dependencia
 * saliente de ms-programacion, reflejando la relacion Clase -> Entrenador
 * que en el monolito era un @ManyToOne JPA.
 */
@Component
public class PersonalClient {

    private final RestClient restClient;

    public PersonalClient(@Qualifier("personalRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public EntrenadorDTO obtenerEntrenador(Long entrenadorId) {
        try {
            return restClient.get()
                    .uri("/api/entrenadores/{id}", entrenadorId)
                    .retrieve()
                    .body(EntrenadorDTO.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El entrenador " + entrenadorId + " no existe en ms-personal");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Error consultando ms-personal: " + e.getMessage());
        }
    }
}

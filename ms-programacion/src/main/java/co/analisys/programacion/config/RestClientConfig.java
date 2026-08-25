package co.analisys.programacion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${personal.service.url}")
    private String personalServiceUrl;

    @Bean
    public RestClient personalRestClient() {
        return RestClient.builder()
                .baseUrl(personalServiceUrl)
                .build();
    }
}

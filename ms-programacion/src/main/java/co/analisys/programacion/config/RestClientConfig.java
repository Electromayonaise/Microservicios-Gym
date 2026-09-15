package co.analisys.programacion.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {

    @Value("${personal.service.url}")
    private String personalServiceUrl;

    @Bean
    public RestClient personalRestClient() {
        return RestClient.builder()
                .baseUrl(personalServiceUrl)
                .requestInterceptor((request, body, execution) -> {
                    ServletRequestAttributes attributes =
                            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest currentRequest = attributes.getRequest();
                        String authorization = currentRequest.getHeader(HttpHeaders.AUTHORIZATION);
                        if (authorization != null) {
                            request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
                        }
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}

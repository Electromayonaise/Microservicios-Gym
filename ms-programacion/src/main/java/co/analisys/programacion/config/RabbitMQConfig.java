package co.analisys.programacion.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Deben coincidir exactamente con las mismas constantes en
    // ms-personal/.../config/RabbitMQConfig.java — no hay modulo compartido.
    public static final String PROGRAMACION_EXCHANGE = "programacion.exchange";
    public static final String CLASE_HORARIO_CAMBIADO_ROUTING_KEY = "clase.horario.cambiado";

    @Bean
    public TopicExchange programacionExchange() {
        return new TopicExchange(PROGRAMACION_EXCHANGE);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}

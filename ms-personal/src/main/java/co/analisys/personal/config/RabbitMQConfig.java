package co.analisys.personal.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // PROGRAMACION_EXCHANGE y CLASE_HORARIO_CAMBIADO_ROUTING_KEY deben coincidir
    // exactamente con las mismas constantes en
    // ms-programacion/.../config/RabbitMQConfig.java — no hay modulo compartido.
    public static final String PROGRAMACION_EXCHANGE = "programacion.exchange";
    public static final String HORARIO_CLASE_QUEUE = "horario.clase.queue";
    public static final String CLASE_HORARIO_CAMBIADO_ROUTING_KEY = "clase.horario.cambiado";

    @Bean
    public TopicExchange programacionExchange() {
        return new TopicExchange(PROGRAMACION_EXCHANGE);
    }

    @Bean
    public Queue horarioClaseQueue() {
        return new Queue(HORARIO_CLASE_QUEUE, true);
    }

    @Bean
    public Binding horarioClaseBinding(Queue horarioClaseQueue, TopicExchange programacionExchange) {
        return BindingBuilder.bind(horarioClaseQueue).to(programacionExchange).with(CLASE_HORARIO_CAMBIADO_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}

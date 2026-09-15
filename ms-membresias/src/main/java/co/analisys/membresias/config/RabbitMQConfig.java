package co.analisys.membresias.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MEMBRESIAS_EXCHANGE = "membresias.exchange";
    public static final String NOTIFICACION_INSCRIPCION_QUEUE = "notificacion.inscripcion.queue";
    public static final String MIEMBRO_INSCRITO_ROUTING_KEY = "miembro.inscrito";

    @Bean
    public TopicExchange membresiasExchange() {
        return new TopicExchange(MEMBRESIAS_EXCHANGE);
    }

    @Bean
    public Queue notificacionInscripcionQueue() {
        return new Queue(NOTIFICACION_INSCRIPCION_QUEUE, true);
    }

    @Bean
    public Binding notificacionInscripcionBinding(Queue notificacionInscripcionQueue, TopicExchange membresiasExchange) {
        return BindingBuilder.bind(notificacionInscripcionQueue).to(membresiasExchange).with(MIEMBRO_INSCRITO_ROUTING_KEY);
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

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        return factory;
    }
}

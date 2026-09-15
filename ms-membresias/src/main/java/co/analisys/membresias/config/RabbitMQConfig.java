package co.analisys.membresias.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
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

    public static final String PAGOS_EXCHANGE = "pagos.exchange";
    public static final String PAGOS_DLX = "pagos.dlx";
    public static final String PAGOS_PROCESAR_QUEUE = "pagos.procesar.queue";
    public static final String PAGOS_DLQ = "pagos.dlq";
    public static final String PAGO_PROCESAR_ROUTING_KEY = "pago.procesar";
    public static final String PAGO_FALLIDO_ROUTING_KEY = "pago.fallido";

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
    public DirectExchange pagosExchange() {
        return new DirectExchange(PAGOS_EXCHANGE);
    }

    @Bean
    public DirectExchange pagosDlx() {
        return new DirectExchange(PAGOS_DLX);
    }

    @Bean
    public Queue pagosProcesarQueue() {
        return QueueBuilder.durable(PAGOS_PROCESAR_QUEUE)
                .withArgument("x-dead-letter-exchange", PAGOS_DLX)
                .withArgument("x-dead-letter-routing-key", PAGO_FALLIDO_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue pagosDlq() {
        return new Queue(PAGOS_DLQ, true);
    }

    @Bean
    public Binding pagosProcesarBinding(Queue pagosProcesarQueue, DirectExchange pagosExchange) {
        return BindingBuilder.bind(pagosProcesarQueue).to(pagosExchange).with(PAGO_PROCESAR_ROUTING_KEY);
    }

    @Bean
    public Binding pagosDlqBinding(Queue pagosDlq, DirectExchange pagosDlx) {
        return BindingBuilder.bind(pagosDlq).to(pagosDlx).with(PAGO_FALLIDO_ROUTING_KEY);
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
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}

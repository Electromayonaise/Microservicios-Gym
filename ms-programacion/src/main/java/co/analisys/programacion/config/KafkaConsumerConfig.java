package co.analisys.programacion.config;

import co.analisys.programacion.messaging.dto.OcupacionClaseEvento;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        // Un registro por poll(): tras un seek-to-0 (recovery, ver
        // AdminKafkaController), cada mensaje reprocesado requiere su propio
        // round-trip de fetch al broker en vez de traer todo el historial en
        // un unico poll(). Junto con MANUAL_IMMEDIATE, esto deja el progreso
        // del reproceso genuinamente observable offset a offset en vez de
        // saltar de una vez al offset final.
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, OcupacionClaseEvento.class);
        config.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // MANUAL_IMMEDIATE (no MANUAL): cada acknowledge() commitea de inmediato
        // en vez de esperar a que termine de procesar el lote completo del poll
        // actual. Con MANUAL, el reproceso completo tras un seek-to-0 (recovery,
        // ver AdminKafkaController) recommitea el offset final en un unico commit
        // atomico, sin que el checkpoint intermedio llegue nunca a ser visible
        // externamente. MANUAL_IMMEDIATE deja el offset commiteado como un
        // checkpoint fiel al progreso real de reproceso, mensaje a mensaje.
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // seekToBeginning() (ver AdminKafkaController) encola el seek, pero un
        // consumidor ocioso solo lo recoge al iniciar su siguiente poll(): con
        // el pollTimeout por defecto (5s) un reinicio puede tardar hasta 5s en
        // siquiera empezar a reprocesar. Bajarlo a 1s acota esa latencia y
        // hace que el mecanismo de recuperacion sea genuinamente responsivo.
        factory.getContainerProperties().setPollTimeout(1000);
        factory.setConcurrency(3);
        return factory;
    }
}

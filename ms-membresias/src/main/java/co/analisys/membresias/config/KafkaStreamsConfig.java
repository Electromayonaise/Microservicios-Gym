package co.analisys.membresias.config;

import co.analisys.membresias.messaging.dto.DatoEntrenamientoEvento;
import co.analisys.membresias.messaging.dto.ResumenEntrenamientoEvento;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "ms-membresias-streams");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return new KafkaStreamsConfiguration(props);
    }

    @Bean
    public StreamsBuilderFactoryBeanConfigurer streamsUncaughtExceptionHandlerConfigurer() {
        return factoryBean -> factoryBean.setStreamsUncaughtExceptionHandler(exception -> {
            System.out.println("Kafka Streams (ms-membresias-streams): excepcion no capturada en el hilo, se reemplaza el hilo: " + exception.getMessage());
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
        });
    }

    @Bean
    public KStream<String, ResumenEntrenamientoEvento> procesarDatosEntrenamiento(StreamsBuilder streamsBuilder) {
        JsonSerde<DatoEntrenamientoEvento> datoSerde = new JsonSerde<>(DatoEntrenamientoEvento.class);
        JsonSerde<ResumenAcumulador> acumuladorSerde = new JsonSerde<>(ResumenAcumulador.class);
        JsonSerde<ResumenEntrenamientoEvento> resumenSerde = new JsonSerde<>(ResumenEntrenamientoEvento.class);

        KStream<String, ResumenEntrenamientoEvento> resumenStream = streamsBuilder
                .stream(KafkaProducerConfig.DATOS_ENTRENAMIENTO_TOPIC, Consumed.with(Serdes.String(), datoSerde))
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)))
                .aggregate(
                        ResumenAcumulador::vacio,
                        (miembroId, dato, acumulador) -> acumulador.acumular(dato),
                        Materialized.with(Serdes.String(), acumuladorSerde))
                .toStream()
                .map((ventana, acumulador) -> KeyValue.pair(ventana.key(), new ResumenEntrenamientoEvento(
                        acumulador.miembroId(),
                        acumulador.totalDuracionMinutos(),
                        acumulador.totalCalorias(),
                        acumulador.cantidadSesiones(),
                        Instant.ofEpochMilli(ventana.window().start()),
                        Instant.ofEpochMilli(ventana.window().end()))));

        resumenStream.to(KafkaProducerConfig.ENTRENAMIENTO_RESUMEN_TOPIC, Produced.with(Serdes.String(), resumenSerde));
        return resumenStream;
    }
}

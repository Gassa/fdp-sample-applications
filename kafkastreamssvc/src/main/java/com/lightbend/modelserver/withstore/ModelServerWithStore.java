package com.lightbend.modelserver.withstore;

import com.lightbend.configuration.AppConfig;
import com.lightbend.configuration.AppParameters;
import com.lightbend.modelserver.store.ModelStateSerde;
import com.lightbend.modelserver.store.ModelStateStoreSupplier;
import com.lightbend.modelserver.store.StoreState;
import com.lightbend.queriablestate.QueriesRestService;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStreamBuilder;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;


@SuppressWarnings("Duplicates")
public class ModelServerWithStore {

    public static void main(String [ ] args) throws Throwable {

        System.out.println("Kafka Streams Model server: "+AppConfig.stringify());

        Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "interactive-queries-example");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "interactive-queries-example-client");
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, AppConfig.KAFKA_BROKER);
        // Provide the details of our embedded http service that we'll use to connect to this streams
        // instance and discover locations of stores.
        streamsConfiguration.put(StreamsConfig.APPLICATION_SERVER_CONFIG, "localhost:" + AppConfig.QUERIABLE_STATE_PORT);
        final File example = Files.createTempDirectory(new File("/tmp").toPath(), "example").toFile();
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, example.getPath());
        // Create topology
        final KafkaStreams streams = createStreams(streamsConfiguration);
        streams.cleanUp();
        streams.start();
        // Start the Restful proxy for servicing remote access to state stores
        final QueriesRestService restService = startRestProxy(streams, AppConfig.QUERIABLE_STATE_PORT);
        // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                streams.close();
                restService.stop();
            } catch (Exception e) {
                // ignored
            }
        }));
    }

    static KafkaStreams createStreams(final Properties streamsConfiguration) {

        Serde<StoreState> stateSerde = new ModelStateSerde();
        ByteArrayDeserializer deserializer = new ByteArrayDeserializer();
        ModelStateStoreSupplier storeSupplier = new ModelStateStoreSupplier("modelStore", stateSerde);


        KStreamBuilder builder = new KStreamBuilder();
        // Data input streams

        builder.addSource("data-source", deserializer, deserializer, AppParameters.DATA_TOPIC)
                .addProcessor("ProcessData", DataProcessorWithStore::new, "data-source");
        builder.addSource("model-source", deserializer, deserializer, AppParameters.MODELS_TOPIC)
                .addProcessor("ProcessModels", ModelProcessorWithStore::new, "model-source");
        builder.addStateStore(storeSupplier, "ProcessData", "ProcessModels");


        return new KafkaStreams(builder, streamsConfiguration);
    }

    static QueriesRestService startRestProxy(final KafkaStreams streams, final int port) throws Exception {
        final QueriesRestService restService = new QueriesRestService(streams);
        restService.start(port);
        return restService;
    }
}

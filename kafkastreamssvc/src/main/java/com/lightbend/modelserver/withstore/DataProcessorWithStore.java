package com.lightbend.modelserver.withstore;

import com.lightbend.configuration.AppConfig;
import com.lightbend.configuration.GrafanaClient;
import com.lightbend.configuration.InfluxDBClient;
import com.lightbend.model.DataConverter;
import com.lightbend.model.Winerecord;
import com.lightbend.modelserver.store.ModelStateStore;
import com.lightbend.queriablestate.ModelServingInfo;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.apache.kafka.streams.processor.ProcessorContext;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by boris on 7/12/17.
 * used
 * https://github.com/bbejeck/kafka-streams/blob/master/src/main/java/bbejeck/processor/stocks/StockSummaryProcessor.java
 */
public class DataProcessorWithStore extends AbstractProcessor<byte[], byte[]> {

    private ModelStateStore modelStore;
    private ProcessorContext context;
    private InfluxDBClient client;

    @Override
    public void process(byte[] key, byte[] value) {
        Optional<Winerecord.WineRecord> dataRecord = DataConverter.convertData(value);
        if(!dataRecord.isPresent()) {
            return;                                 // Bad record
        }
        if(modelStore.getNewModel() != null){
            // update the model
            if(modelStore.getCurrentModel() != null)
                modelStore.getCurrentModel().cleanup();
            modelStore.setCurrentModel(modelStore.getNewModel());
            modelStore.setCurrentServingInfo(new ModelServingInfo(modelStore.getNewServingInfo().getName(),
                    modelStore.getNewServingInfo().getDescription(), System.currentTimeMillis()));
            modelStore.setNewServingInfo(null);
            modelStore.setNewModel(null);
        }
        // Actually score
        if(modelStore.getCurrentModel() == null) {
            // No model currently
            System.out.println("No model available - skipping");
        }
        else{
            // Score the model
            long start = System.currentTimeMillis();
            double quality = (double) modelStore.getCurrentModel().score(dataRecord.get());
            long duration = System.currentTimeMillis() - start;
            modelStore.getCurrentServingInfo().update(duration);
            System.out.println("Calculated quality - " + quality + " in " + duration + "ms");
            client.writePoint("Kafka Streams", modelStore.getCurrentServingInfo().getName(), quality, duration);
         }

    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        modelStore = (ModelStateStore) this.context.getStateStore("modelStore");
        Objects.requireNonNull(modelStore, "State store can't be null");

        // Create InfluxDB connection
        client = new InfluxDBClient(AppConfig.INFLUX_DB_CONFIG);
        new GrafanaClient(AppConfig.GRAFANA_CONFIG, AppConfig.INFLUX_DB_CONFIG);
    }
}

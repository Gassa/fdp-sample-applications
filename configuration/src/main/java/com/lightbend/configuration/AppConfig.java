package com.lightbend.configuration;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class AppConfig {

    public static final Config config = ConfigFactory.load();

    public static final String ZOOKEEPER_HOST = config.getString("zookeeper.hosts");
    public static final String KAFKA_BROKER = config.getString("kafka.brokers");
    public static final InfluxDBConfig INFLUX_DB_CONFIG = new InfluxDBConfig(config.getConfig("influxdb"));
    public static final GrafanaConfig GRAFANA_CONFIG = new GrafanaConfig(config.getConfig("grafana"));
    public static final int MODEL_SERVER_PORT = config.getInt("model_server.port");
    public static final int QUERIABLE_STATE_PORT = config.getInt("queriable_state.port");
    public static final String ASSIGNED_PORT = config.getString("task_debug.port");

    static {
        System.out.println("Assigned port: "+ASSIGNED_PORT);
    }

    private AppConfig() {}

    public static String stringify() {
        return "kafka brokers:"+ KAFKA_BROKER + " Zookeeper: "+ ZOOKEEPER_HOST +
               " InfluxDB:"+ INFLUX_DB_CONFIG + " Grafana:" + GRAFANA_CONFIG;
    }

}

package com.lightbend.killrweather.settings

import org.scalatest.{ Matchers, WordSpec }

class WeatherSettingsTest extends WordSpec with Matchers {

  "WeatherSettings" should {
    val ws = new WeatherSettings()

    "Load the default Kafka configuration" in {
      ws.kafkaConfig.brokers should be("unit-test-host") // from test override
      ws.kafkaConfig.group should be("killrweather.group") // from reference
      ws.kafkaConfig.topic should be("killrweather.raw") // from reference
    }

    "Load the default Spark configuration" in {
      val sparkConf = ws.sparkConfig
      sparkConf should contain("spark.master" -> "local[2]")
    }

    "Load the default streaming configuration" in {
      ws.streamingConfig.checkpointDir should include("checkpoints")
      ws.streamingConfig.batchInterval.getSeconds should be > 1L
    }

    "Load the default application-bound cassandra configuration" in {
      ws.CassandraKeyspace should not be ('empty)
    }

    "Load the default influx db configuration" in {
      ws.influxDBServer should not be ('empty)
    }

  }

}

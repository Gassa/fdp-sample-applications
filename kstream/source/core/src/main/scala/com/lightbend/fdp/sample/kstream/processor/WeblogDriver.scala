package com.lightbend.fdp.sample.kstream
package processor

import java.util.Properties

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.util.{ Success, Failure }

import org.apache.kafka.streams.processor.{ StateStoreSupplier, TopologyBuilder }
import org.apache.kafka.streams.state.{ Stores, HostInfo }
import org.apache.kafka.streams.{ StreamsConfig, KafkaStreams }
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.clients.consumer.ConsumerConfig;

import serializers.Serializers
import config.KStreamConfig._
import http.WeblogMicroservice

object WeblogDriver extends LazyLogging with Serializers {

  final val LOG_COUNT_STATE_STORE = "log-counts"

  def main(args: Array[String]): Unit = {

    var restService: WeblogMicroservice = null

    // get config info
    val config: ConfigData = fromConfig(ConfigFactory.load()) match {
      case Success(c)  => c
      case Failure(ex) => throw ex
    }

    // setup REST endpoints
    val restEndpointPort = config.httpPort
    val restEndpointHostName = config.httpInterface
    val restEndpoint = new HostInfo(restEndpointHostName, restEndpointPort)

    logger.info("Connecting to Kafka cluster via bootstrap servers " + config.brokers)
    logger.info("REST endpoint at http://" + restEndpointHostName + ":" + restEndpointPort)

    val streams = createStreams(config)

    // need to exit for any stream exception
    // mesos will restart the application
    streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      override def uncaughtException(t: Thread, e: Throwable): Unit = try {
        logger.error(s"Stream terminated because of uncaught exception .. Shutting down app", e)
        restService.stop()
        streams.close()
      } catch {
        case _: Exception => 
      } finally {
        System.exit(-1)
      }
    })

    // Need to be done for running the application after resetting the state store
    // should not be done in production
    streams.cleanUp()

    // Now that we have finished the definition of the processing topology we can actually run
    // it via `start()`.  The Streams application as a whole can be launched just like any
    // normal Java application that has a `main()` method.
    streams.start()

    // Start the Restful proxy for servicing remote access to state stores
    restService = startRestProxy(streams, restEndpoint)

    // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
    Runtime.getRuntime().addShutdownHook(new Thread(() => try {
      restService.stop()
      streams.close()
    } catch {
      case _: Exception => // ignored
    }))
  }

  def startRestProxy(streams: KafkaStreams, hostInfo: HostInfo): WeblogMicroservice = {
    val restService = new WeblogMicroservice(streams, hostInfo)
    restService.start()
    restService
  }
  
  def createStreams(config: ConfigData): KafkaStreams = {
    val changelogConfig = {
      val cfg = new java.util.HashMap[String, String]
      val segmentSizeBytes = (20 * 1024 * 1024).toString
      cfg.put("segment.bytes", segmentSizeBytes)
      cfg
    }

    // Kafka stream configuration
    val streamingConfig = {
      val settings = new Properties
      settings.put(StreamsConfig.APPLICATION_ID_CONFIG, "kstream-log-count")
      settings.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.brokers)
      settings.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)
      settings.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass.getName)

      // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
      // Note: To re-run the demo, you need to use the offset reset tool:
      // https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Streams+Application+Reset+Tool
      settings.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

      // need this for query service
      settings.put(StreamsConfig.APPLICATION_SERVER_CONFIG, s"${config.httpInterface}:${config.httpPort}")

      // default is /tmp/kafka-streams
      settings.put(StreamsConfig.STATE_DIR_CONFIG, config.stateStoreDir)

      // Set the commit interval to 500ms so that any changes are flushed frequently and the summary
      // data are updated with low latency.
      settings.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, "500");

      settings
    }

    val builder: TopologyBuilder = new TopologyBuilder()
    builder.addSource("Source", config.fromTopic)
           .addProcessor("Process", WeblogProcessorSupplier, "Source")
           .addStateStore(new BFStoreSupplier[String](LOG_COUNT_STATE_STORE, stringSerde, true, changelogConfig), "Process")

    new KafkaStreams(builder, streamingConfig)
  }
}

import org.apache.kafka.streams.processor.ProcessorSupplier
object WeblogProcessorSupplier extends ProcessorSupplier[String, String] {
  override def get(): WeblogProcessor = new WeblogProcessor()
}

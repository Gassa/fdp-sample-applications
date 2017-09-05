package com.lightbend.fdp.sample.kstream

import com.typesafe.config.ConfigFactory

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.state.HostInfo

import scala.util.{ Success, Failure }
import scala.concurrent.duration._
import sys.process._
import com.typesafe.scalalogging.LazyLogging

import config.KStreamConfig._
import serializers._

import http.InteractiveQueryHttpService

import ingestion.DataIngestion

trait WeblogWorkflow extends LazyLogging with Serializers {

  def workflow(): Unit = {
    
    // get config info
    val config: ConfigData = fromConfig(ConfigFactory.load()) match {
      case Success(c)  => c
      case Failure(ex) => throw ex
    }

    logger.info(s"config = $config")
    config.schemaRegistryUrl.foreach { url =>
      logger.info(s"Schema Registry will be used - please ensure schema registry service is up and running at $url")
    }

    // setup REST endpoints
    val restEndpointPort = config.httpPort
    val restEndpointHostName = config.httpInterface
    val restEndpoint = new HostInfo(restEndpointHostName, restEndpointPort)
    
    logger.info("Connecting to Kafka cluster via bootstrap servers " + config.brokers)
    logger.info("REST endpoint at http://" + restEndpointHostName + ":" + restEndpointPort)

    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()

    import system.dispatcher

    // register for data ingestion
    // whenever we find new / changed files in the configured location, we run data loading
    // However `directoryToWatch` may not be set if we are trying to run the application in
    // distributed mode with multiple instances. In that case only one instance will do the ingestion
    // and for subsequent instances of the application, we don't need to do the ingestion.
    // Ingestion can be done only from one instance
    config.directoryToWatch.foreach { d =>
      DataIngestion.registerForIngestion(config)

      // schedule a run by touching the data folder
      system.scheduler.scheduleOnce(1 minute) {
        Seq("/bin/sh", "-c", s"touch $d/*").!
        ()
      }
    }


    // set up the topology
    val streams: KafkaStreams = createStreams(config)

    // Need to be done for running the application after resetting the state store
    // should not be done in production
    streams.cleanUp()

    // Now that we have finished the definition of the processing topology we can actually run
    // it via `start()`.  The Streams application as a whole can be launched just like any
    // normal Java application that has a `main()` method.
    streams.start()

    // Start the Restful proxy for servicing remote access to state stores
    val restService = startRestProxy(streams, restEndpoint, system, materializer)

    // need to exit for any stream exception
    // mesos will restart the application
    streams.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      override def uncaughtException(t: Thread, e: Throwable): Unit = try {
        logger.error(s"Stream terminated because of uncaught exception .. Shutting down app", e)
        restService.stop()
        logger.error(s"Stopping streams service ..")
        val closed = streams.close(1, TimeUnit.MINUTES)
        logger.error(s"Exiting application after streams close ($closed)")
      } catch {
        case _: Exception => 
      } finally {
        logger.error("Exiting application ..")
        System.exit(-1)
      }
    })

    // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
    Runtime.getRuntime().addShutdownHook(new Thread(() => try {
      restService.stop()
      val closed = streams.close(1, TimeUnit.MINUTES)
      logger.error(s"Exiting application after streams close ($closed)")
    } catch {
      case _: Exception => // ignored
    }))
  }  

  def createStreams(config: ConfigData): KafkaStreams
  def startRestProxy(streams: KafkaStreams, hostInfo: HostInfo,
    actorSystem: ActorSystem, materializer: ActorMaterializer): InteractiveQueryHttpService
}

package com.lightbend.fdp.sample.kstream
package config

import cats._
import cats.data._
import cats.instances.all._

import scala.util.Try
import com.typesafe.config.Config
import scala.concurrent.duration._


/**
 * This object wraps the native Java config APIs into a monadic
 * interpreter
 */ 
object KStreamConfig {

  private[KStreamConfig] case class KafkaSettings(
    brokers: String, 
    zk: String, 
    fromTopic: String, 
    errorTopic: String,
    toTopic: Option[String], 
    summaryAccessTopic: Option[String], 
    windowedSummaryAccessTopic: Option[String], 
    summaryPayloadTopic: Option[String], 
    windowedSummaryPayloadTopic: Option[String],
    stateStoreDir: String
  )

  private[KStreamConfig] case class HttpSettings(
    interface: String,
    port: Int
  )

  private[KStreamConfig] case class DataLoaderSettings(
    sourceTopic: String,
    directoryToWatch: String,
    pollInterval: FiniteDuration
  )

  case class ConfigData(ks: KafkaSettings, hs: HttpSettings, dls: DataLoaderSettings) {
    def brokers = ks.brokers
    def zk = ks.zk
    def fromTopic = ks.fromTopic
    def toTopic = ks.toTopic
    def summaryAccessTopic = ks.summaryAccessTopic
    def windowedSummaryAccessTopic = ks.windowedSummaryAccessTopic
    def summaryPayloadTopic = ks.summaryPayloadTopic
    def windowedSummaryPayloadTopic = ks.windowedSummaryPayloadTopic
    def errorTopic = ks.errorTopic
    def stateStoreDir = ks.stateStoreDir
    def httpInterface = hs.interface
    def httpPort = hs.port
    def sourceTopic = dls.sourceTopic
    def directoryToWatch = dls.directoryToWatch
    def pollInterval = dls.pollInterval
  }

  type ConfigReader[A] = ReaderT[Try, Config, A]

  private def getStringMaybe(config: Config, key: String): Option[String] = try {
    Some(config.getString(key))
  } catch {
    case _: Exception => None
  }

  private def fromKafkaConfig: ConfigReader[KafkaSettings] = Kleisli { (config: Config) =>
    Try {
      KafkaSettings(
        config.getString("dcos.kafka.brokers"),
        config.getString("dcos.kafka.zookeeper"),
        config.getString("dcos.kafka.fromtopic"),
        config.getString("dcos.kafka.errortopic"),
        getStringMaybe(config, "dcos.kafka.totopic"),
        getStringMaybe(config, "dcos.kafka.summaryaccesstopic"),
        getStringMaybe(config, "dcos.kafka.windowedsummaryaccesstopic"),
        getStringMaybe(config, "dcos.kafka.summarypayloadtopic"),
        getStringMaybe(config, "dcos.kafka.windowedsummarypayloadtopic"),
        config.getString("dcos.kafka.statestoredir")
      )
    }
  }

  private def fromHttpConfig: ConfigReader[HttpSettings] = Kleisli { (config: Config) =>
    Try {
      HttpSettings(
        config.getString("dcos.http.interface"),
        config.getInt("dcos.http.port")
      )
    }
  }

  private def fromDataLoaderConfig: ConfigReader[DataLoaderSettings] = Kleisli { (config: Config) =>
    Try {
      DataLoaderSettings(
        config.getString("dcos.kafka.loader.sourcetopic"),
        config.getString("dcos.kafka.loader.directorytowatch"),
        config.getDuration("dcos.kafka.loader.pollinterval")
      )
    }
  }

  def fromConfig: ConfigReader[ConfigData] = for {
    k <- fromKafkaConfig
    h <- fromHttpConfig
    d <- fromDataLoaderConfig
  } yield ConfigData(k, h, d)
}


/**
  * Created by boris on 7/14/17.
  */
import sbt._
import Versions._

object Dependencies {
  val reactiveKafka = "com.typesafe.akka"               % "akka-stream-kafka_2.11"        % reactiveKafkaVersion
  
  val akkaStream    = "com.typesafe.akka"               % "akka-stream_2.11"              % akkaVersion
  val akkaHttp      = "com.typesafe.akka"               % "akka-http_2.11"                % akkaHttpVersion
  val akkaHttpJsonJackson = "de.heikoseeberger"         % "akka-http-jackson_2.11"        % akkaHttpJsonVersion


  val kafka         = "org.apache.kafka"                % "kafka_2.11"                    % kafkaVersion
  val kafkaclients  = "org.apache.kafka"                % "kafka-clients"                 % kafkaVersion
  val kafkastreams  = "org.apache.kafka"                % "kafka-streams"                 % kafkaVersion

  val curator       = "org.apache.curator"              % "curator-test"                  % Curator                 // ApacheV2

  val gson          = "com.google.code.gson"            % "gson"                          % gsonVersion
  val jersey        = "org.glassfish.jersey.containers" % "jersey-container-servlet-core" % jerseyVersion
  val jerseymedia   = "org.glassfish.jersey.media"      % "jersey-media-json-jackson"     % jerseyVersion
  val jettyserver   = "org.eclipse.jetty"               % "jetty-server"                  % jettyVersion
  val jettyservlet  = "org.eclipse.jetty"               % "jetty-servlet"                 % jettyVersion
  val wsrs          = "javax.ws.rs"                     % "javax.ws.rs-api"               % wsrsVersion

  val tensorflow    = "org.tensorflow"                  % "tensorflow"                    % tensorflowVersion

  val jpmml         = "org.jpmml"                       % "pmml-evaluator"                % PMMLVersion
  val jpmmlextras   = "org.jpmml"                       % "pmml-evaluator-extension"      % PMMLVersion

  val influxDBClient    = "org.influxdb"            % "influxdb-java"                   % influxDBClientVersion

  val codecBase64   = "commons-codec"               % "commons-codec"                   % codecVersion
  val typesafeConfig    = "com.typesafe"            %  "config"                         % TypesafeConfigVersion

  val configuration = Seq(typesafeConfig)
  val modelsDependencies    = Seq(jpmml, jpmmlextras, tensorflow)
  val kafkabaseDependencies = configuration ++ Seq(kafka, kafkaclients, reactiveKafka)
  val kafkaDependencies     = Seq(reactiveKafka, kafka, kafkaclients, kafkastreams)
  val webDependencies       = Seq(gson, jersey, jerseymedia, jettyserver, jettyservlet, wsrs)
  val akkaServerDependencies = Seq(reactiveKafka, akkaStream, akkaHttp, akkaHttpJsonJackson, reactiveKafka)

}

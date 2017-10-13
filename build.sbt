import Dependencies._
import deployssh.DeploySSH._

lazy val protobufs = (project in file("./protobufs"))
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ))
  .settings((allowSnapshot in ThisBuild) := true)  // Force builds to work!!!!
  .settings(libraryDependencies ++= grpc)
  .settings(dependencyOverrides += "io.netty" % "netty-codec-http2" % "4.1.11.Final")
  .settings(dependencyOverrides += "io.netty" % "netty-handler-proxy" % "4.1.11.Final")

lazy val killrWeatherCore = (project in file("./killrweather-core"))
  .settings(defaultSettings:_*)
  .settings((allowSnapshot in ThisBuild) := true)  // Force builds to work!!!!
  .settings(libraryDependencies ++= core)


lazy val killrWeatherApp = (project in file("./killrweather-app"))
  .settings(defaultSettings:_*)
  .settings(
    mainClass in Compile := Some("com.lightbend.killrweather.app.KillrWeather"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "KillrWeather Spark Runner",
    packageDescription := "KillrWeather Spark Runner",
    (allowSnapshot in ThisBuild) := true,  // Force builds to work!!!!
    libraryDependencies ++= app)
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core"  % "2.6.7")
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7")
  .settings(dependencyDotFile := file("dependencies.dot"))
  .settings(
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "KillrWeather Spark uber jar",
    packageDescription := "KillrWeather Spark uber jar",
//    assemblyJarName in assembly := "killrweather-spark.jar",
    mainClass in assembly := Some("com.lightbend.killrweather.app.KillrWeather"),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.last
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
        ArtifactSSH(assembly.value, "/var/www/html/")
    )
  )
  .dependsOn(killrWeatherCore, protobufs)
  .enablePlugins(DeploySSH)

lazy val appLocalRunner = (project in file("./killrweather-app-local"))
  .settings(
    (allowSnapshot in ThisBuild) := true,  // Force builds to work!!!!
    libraryDependencies ++= spark.map(_.copy(configurations = Option("compile"))) ++ Seq(influxDBClient)
  )
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core"  % "2.6.7")
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7")
  .dependsOn(killrWeatherApp)

lazy val httpclient = (project in file("./killrweather-httpclient"))
  .settings(defaultSettings:_*)
  .settings(
    buildInfoPackage := "build",
    mainClass in Compile := Some("com.lightbend.killrweather.client.http.RestAPIs"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "KillrWeather HTTP client",
    packageDescription := "KillrWeather HTTP client",
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
      ArtifactSSH((packageZipTarball in Universal).value, "/var/www/html/")
    ),
    (allowSnapshot in ThisBuild) := true,  // Force builds to work!!!!
    libraryDependencies ++= clientHTTP)
  .dependsOn(killrWeatherCore, protobufs)
  .enablePlugins(DeploySSH)
  .enablePlugins(JavaAppPackaging)

lazy val grpcclient = (project in file("./killrweather-grpclient"))
  .settings(defaultSettings:_*)
  .settings(
    buildInfoPackage := "build",
    mainClass in Compile := Some("com.lightbend.killrweather.client.grpc.WeatherGRPCClient"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "KillrWeather GRPC client",
    packageDescription := "KillrWeather GRPC client",
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
      ArtifactSSH((packageZipTarball in Universal).value, "/var/www/html/")
    ),
    (allowSnapshot in ThisBuild) := true,  // Force builds to work!!!!
    libraryDependencies ++= clientGRPC)
  .dependsOn(killrWeatherCore, protobufs)
  .enablePlugins(DeploySSH)
  .enablePlugins(JavaAppPackaging)

lazy val loader = (project in file("./killrweather-loader"))
  .settings(defaultSettings:_*)
  .settings(
    buildInfoPackage := "build",
    mainClass in Compile := Some("com.lightbend.killrweather.loader.kafka.KafkaDataIngester"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "KillrWeather loaders",
    packageDescription := "KillrWeather loaders",
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
      ArtifactSSH((packageZipTarball in Universal).value, "/var/www/html/")
    ),
    (allowSnapshot in ThisBuild) := true,  // Force builds to work!!!!
    libraryDependencies ++= loaders)
  .dependsOn(killrWeatherCore, protobufs)
  .enablePlugins(DeploySSH)
  .enablePlugins(JavaAppPackaging)

lazy val root = (project in file("."))
  .aggregate(killrWeatherCore, killrWeatherApp, httpclient, grpcclient, loader, protobufs)


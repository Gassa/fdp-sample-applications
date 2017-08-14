name := "KillrWeather"

version := "1.0"

scalaVersion in ThisBuild := "2.11.11"

lazy val protobufs = (project in file("./protobufs"))
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ))
  .settings(libraryDependencies ++= Dependencies.grpc)
  .settings(dependencyOverrides += "io.netty" % "netty-codec-http2" % "4.1.11.Final")
  .settings(dependencyOverrides += "io.netty" % "netty-handler-proxy" % "4.1.11.Final")

lazy val core = (project in file("./killrweather-core"))
  .settings(defaultSettings:_*)
  .settings(libraryDependencies ++= Dependencies.core)

lazy val app = (project in file("./killrweather-app"))
  .settings(defaultSettings:_*)
  .settings(libraryDependencies ++= Dependencies.app)
  .dependsOn(core, protobufs)

lazy val clients = (project in file("./killrweather-clients"))
  .settings(defaultSettings:_*)
  .settings(libraryDependencies ++= Dependencies.client)
  .dependsOn(core, protobufs)

lazy val root = (project in file(".")).
  aggregate(core, app, clients, protobufs)


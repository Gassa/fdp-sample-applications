resolvers += "Bintray Repository" at "https://dl.bintray.com/shmishleniy/"

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")
addSbtPlugin("com.lightbend" % "sbt-whitesource" % "0.1.5")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0")

addSbtPlugin("com.github.shmishleniy" %% "sbt-deploy-ssh" % "0.1.3")
addSbtPlugin("com.cavorite" % "sbt-avro-1-7" % "1.1.2")

addSbtPlugin("com.dwijnand" % "sbt-dynver" % "2.0.0")

dependsOn(RootProject(file("../../../build-plugin/").toURI))
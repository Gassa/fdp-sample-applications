resolvers += "Bintray Repository" at "https://dl.bintray.com/shmishleniy/"

resolvers += "JAnalyse Repository" at "http://www.janalyse.fr/repository/"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

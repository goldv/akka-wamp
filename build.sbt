name := "akka-wamp"

version       := "1.0-SNAPSHOT"

scalaVersion  := "2.11.6"

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Seq(
    "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-RC4",
    "com.typesafe.akka" % "akka-http-core-experimental_2.11" % "1.0-RC4",
    "com.typesafe.akka" % "akka-http-experimental_2.11" % "1.0-RC4",
    "com.typesafe.akka" %% "akka-testkit" % "2.3.11" % "test",
    "com.typesafe.play" %% "play-json" % "2.3.9",
    "org.slf4j" % "slf4j-api" % "1.6.1",
    "org.slf4j" % "slf4j-simple" % "1.6.1",
    "org.specs2" %% "specs2-core" % "3.6.2" % "test",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)



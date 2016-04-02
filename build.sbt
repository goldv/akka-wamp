name := "akka-wamp"

organization := "akka-wamp"

version       := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.2",
    "com.typesafe.akka" %% "akka-stream" % "2.4.2",
    "com.typesafe.akka" %% "akka-http-experimental" % "2.4.2",
    "com.typesafe.akka" %% "akka-testkit" % "2.4.2" % "test",
    "com.typesafe.play" %% "play-json" % "2.4.2",
    "org.slf4j" % "slf4j-api" % "1.6.1",
    "org.slf4j" % "slf4j-simple" % "1.6.1",
    "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)



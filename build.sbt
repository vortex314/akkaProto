name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.6"
scalacOptions += "-target:jvm-1.8"

lazy val akkaVersion = "2.5.13"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "0.20"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.13",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.13" % Test
)
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "0.20"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "0.20"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.13",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.13" % Test
)
// https://mvnrepository.com/artifact/com.googlecode.protobuf-java-format/protobuf-java-format
libraryDependencies += "com.googlecode.protobuf-java-format" % "protobuf-java-format" % "1.4"
// https://mvnrepository.com/artifact/org.apache.avro/avro
libraryDependencies += "org.apache.avro" % "avro" % "1.8.2"
// https://mvnrepository.com/artifact/com.bazaarvoice.jolt/jolt-core
libraryDependencies += "com.bazaarvoice.jolt" % "jolt-core" % "0.1.1"
// https://mvnrepository.com/artifact/com.bazaarvoice.jolt/json-utils
libraryDependencies += "com.bazaarvoice.jolt" % "json-utils" % "0.1.1"
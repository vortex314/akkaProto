package be.limero

import java.nio.file.Paths

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSourceSettings}
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source}
import akka.util.ByteString
import be.limero.Bus.PropertyChange
import com.typesafe.config.{ConfigObject, ConfigValue}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.concurrent.duration._
import scala.concurrent.Future

object MqttStream {
  implicit val system = ActorSystem("limero")
  val config = system.settings.config
  val log = system.log

  def main(args: Array[String]): Unit = {
    println("Hello, world!")
    val mqttStream = new MqttStream
  }

  class BusMessage(topic: Topic)

  case class PropertyChange(topic: Topic, payload: String) extends BusMessage(topic)

  case class Command(topic: Topic, payload: String) extends BusMessage(topic)

  case class Location( x,y : Double )

}

class MqttStream {

  import MqttStream._

  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s ⇒ ByteString(s + "\n"))
      .toMat(FileIO.toPath(Paths.get(filename)))(Keep.right)

  val connectionSettings = MqttConnectionSettings(
    "tcp://limero.ddns.net:1883",
    "test-scala-client",
    new MemoryPersistence,
    automaticReconnect = true
  )
  // val connectionSettings.automaticReconnect = true


  implicit val materializer = ActorMaterializer()

  val sourceSettings = MqttSourceSettings(connectionSettings, Map("src/#" -> MqttQoS.AtLeastOnce))
  val mqttSource: Source[MqttMessage, Future[Done]] = MqttSource.atMostOnce(sourceSettings, bufferSize = 1)

  val newSource = mqttSource.filter(msg => msg.topic.matches(".*/dwm1000/.*"))
    .map(msg => new MqttStream.PropertyChange(new Topic(msg.topic), msg.payload.utf8String))

  /*newSource.runForeach(msg => {
    log.info(msg.topic + "=" + msg.payload)
  })(materializer)*/

  val g = RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      import akka.stream.scaladsl.GraphDSL.Implicits._
      val in = newSource
      val out = Sink.ignore

      val bcast = builder.add(Broadcast[MqttStream.PropertyChange](2))
      val merge = builder.add(Merge[MqttStream.PropertyChange](2))

      val f1, f2, f3, f4 = Flow[MqttStream.PropertyChange].filter(pc => pc.topic.matches(""))

      def ff(pattern: String) = Flow[MqttStream.PropertyChange].filter(pc => pc.topic.matches(pattern))

      def logger(location:String) = Flow[MqttStream.PropertyChange].map(pc => {
        log.info("{} {}", location, pc.topic); pc
      })

      in ~>  bcast ~> ff("anchor.*/dwm1000/.*") ~> logger("DWM1000") ~> merge ~> f3 ~> out
              bcast ~> f4 ~> merge
      ClosedShape
  })

  g.run();

}

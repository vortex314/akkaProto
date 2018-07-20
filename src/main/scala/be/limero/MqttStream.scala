package be.limero

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream._
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSourceSettings}
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source}
import akka.util.{ByteString, Timeout}
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

  case class Property(topic: Topic, payload: String) extends BusMessage(topic)

  case class Command(topic: Topic, payload: String) extends BusMessage(topic)

  case class Location(topic: Topic, x: Double, y: Double) extends BusMessage(topic)

  case class Vector(topic: Topic, x: Double, y: Double, speed: Double) extends BusMessage(topic)

  case class WireDetected(topic: Topic, t: Boolean) extends BusMessage(topic)

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
    .map(msg => new MqttStream.Property(new Topic(msg.topic), msg.payload.utf8String))

  val trilat = system.actorOf(TrilaterationActor.props(".*/dwm1000/.*"), "trilaterationActor")


  /*newSource.runForeach(msg => {
    log.info(msg.topic + "=" + msg.payload)
  })(materializer)*/

  val g = RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      import akka.stream.scaladsl.GraphDSL.Implicits._
      val in = newSource
      val out = Sink.ignore

      val bcast = builder.add(Broadcast[MqttStream.Property](2))
      val merge = builder.add(Merge[MqttStream.Property](2))

      val f1, f2, f3, f4 = Flow[MqttStream.Property].filter(pc => pc.topic.matches(""))

      def ff(pattern: String) = Flow[MqttStream.Property].filter(pc => pc.topic.matches(pattern))

      def service(pattern: String) = Flow[MqttStream.Property].filter(pc => pc.topic.service.equals(pattern))

      def device(pattern: String) = Flow[MqttStream.Property].filter(pc => pc.topic.device.equals(pattern))

      def property(pattern: String) = Flow[MqttStream.Property].filter(pc => pc.topic.property.equals(pattern))

      def logger(location: String) = Flow[MqttStream.Property].map(pc => {
        log.info("{} {}", location, pc.topic);
        pc
      })
      implicit val timeout:Timeout = new FiniteDuration(100,TimeUnit.MILLISECONDS)
      def call(actor:ActorRef) = Flow[MqttStream.Property].mapAsync(parallelism = 5)(elem => (actor ? elem).mapTo[MqttStream.Property])

      in ~> bcast
      bcast ~> service("dwm1000") ~> logger("DWM1000") ~> merge ~> f3 ~> out
      bcast ~> service("dwm1000") ~> call(trilat) ~> merge
      ClosedShape
  })

  g.run();

}

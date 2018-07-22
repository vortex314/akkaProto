package be.limero

import akka.actor.{ActorRef, ActorSystem}
import akka.stream._
import akka.stream.alpakka.mqtt.scaladsl.MqttSource
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttQoS, MqttSourceSettings}
import be.limero.Bus.PropertyChange
import com.typesafe.config.{ConfigObject, ConfigValue}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

import scala.collection.JavaConversions._

object MqttProto {
  def main(args: Array[String]): Unit = {
    println("Hello, world!")
    val proto: MqttProto = new MqttProto
    proto.runner
  }
}

class MqttProto {
  val connectionSettings = MqttConnectionSettings(
    "tcp://limero.ddns.net:1883",
    "test-scala-client",
    new MemoryPersistence,
    automaticReconnect = true
  )

  val connectionSettings.automaticReconnect = true

  implicit val system :ActorSystem = ActorSystem("limero")
  implicit val materializer : Materializer = ActorMaterializer()

  val config = system.settings.config

  val log = system.log


  val sourceSettings = MqttSourceSettings(connectionSettings, Map("src/#" -> MqttQoS.AtLeastOnce))

  val mqttSource = MqttSource.atMostOnce(sourceSettings, bufferSize = 1)

  def runner = {
    val actor: ActorRef = system.actorOf(MqttActor.props)

    log.info(config.getString("actors.package"))
    config.getConfig("actors").root.map{
      case (name:String,configObject:ConfigObject) => {
        log.info(" found {} ",name)
        None
      }
      case (name:String,value:ConfigValue) => {log.info(" found  also {} ",name)}
    }
    val configMap = config.getConfig("actors").entrySet().toList.map(entry => (entry.getKey, entry.getValue)).toMap

    log.info(" configMap {}",configMap)

    system.actorOf(BusListenerActor.props(".*"), "buslistener")
    system.actorOf(TrilaterationActor.props(".*/dwm1000/.*"), "trilaterationActor")
    system.actorOf(UptimeListener.props(".*/system/upTime"), "upTime")
    mqttSource.runForeach(msg => {
      log.debug(msg.topic + "=" + msg.payload.utf8String)
      Bus.bus.publish(PropertyChange(new Topic(msg.topic), msg.payload.utf8String))
      actor.tell(PropertyChange(new Topic(msg.topic), msg.payload.utf8String), ActorRef.noSender)
    })(materializer)
  }


}
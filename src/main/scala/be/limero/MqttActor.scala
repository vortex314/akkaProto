package be.limero

import java.util

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.alpakka.mqtt.MqttMessage
import be.limero.Bus.PropertyChange

object MqttActor {
  def props: Props = Props(new MqttActor)
}

class MqttActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case MqttMessage(topic, payload, qos, retained) => log.debug(topic + "=" + payload.utf8String + " from Actor" + qos + "," + retained)
    case PropertyChange(topic, payload) => {
      log.debug(topic.toString)
    }
  }
}

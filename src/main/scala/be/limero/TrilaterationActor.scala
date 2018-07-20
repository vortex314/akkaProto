package be.limero

import java.util

import akka.actor.{Actor, ActorLogging, Props, Timers}
import be.limero.MqttStream.Property
import be.limero.TrilaterationActor.{Tick, TickKey}

import scala.concurrent.duration._

object TrilaterationActor {
  def props: String => Props = pattern => Props(new TrilaterationActor(pattern))

  case object Tick

  case object TickKey

}

class TrilaterationActor(pattern: String) extends Actor with ActorLogging with Timers {

  class Anchor(name: String) {
    var x, y: Double = 0
    var t: Long = System.currentTimeMillis()
    var distance: Double = 0.0

    override def toString: String = name + ":{" + x + "," + y + "," + distance + "}"
  }

  var anchors = new util.HashMap[String, Anchor]
  var updates: Int = 0

  Bus.bus.subscribe(self, pattern)
  timers.startPeriodicTimer(TickKey, Tick, 5.second)


  val findAnchor: String => Anchor = key => {
    if (anchors.containsKey(key)) anchors.get(key)
    else {
      anchors.put(key, new Anchor(key))
      anchors.get(key)
    }
  }

  override def receive: Receive = {
    case Property(topic, payload) => {
      log.debug(" message received ")
      val anchor: Anchor = findAnchor(topic.device)
      if (topic.property == "x") anchor.x = payload.toDouble
      if (topic.property == "y") anchor.y = payload.toDouble
      if (topic.property == "distance") {
        anchor.distance = payload.toDouble
        anchor.t = System.currentTimeMillis()
      }
      updates += 1
      sender.tell(Property(topic,payload),self)
    }
    case Tick => {
      anchors.forEach((key, anchor) => log.info(" anchor {} = {},{},{} ", key, anchor.x, anchor.y, anchor.distance))
      log.info(s" found {} anchors, updates : $updates " , anchors.size())//, updates)
    }
  }
}

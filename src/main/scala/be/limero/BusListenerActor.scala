package be.limero

import akka.actor.{Actor, ActorLogging, Props, Timers}
import Bus.PropertyChange

object BusListenerActor {
  def props(pattern: String): Props = Props(new BusListenerActor(pattern))
}

class BusListenerActor(pattern: String) extends Actor with ActorLogging with Timers {
  Bus.bus.subscribe(self, pattern)
  log.info("started")

  override def receive: Receive = {
    case PropertyChange(topic, payload) => log.debug(topic + ":" + payload )
  }
}

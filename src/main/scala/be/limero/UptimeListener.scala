package be.limero

import akka.actor.{Actor, ActorLogging, Props, Timers}
import be.limero.Bus.PropertyChange
import be.limero.TrilaterationActor.{Tick, TickKey}

import scala.concurrent.duration._

object UptimeListener {
  def props(pattern: String): Props = Props(new UptimeListener(pattern))

}

class UptimeListener(pattern: String) extends Actor with ActorLogging with Timers {
  Bus.bus.subscribe(self, pattern)

  log.info("started")

  timers.startPeriodicTimer(TickKey, Tick, 5.second)

  class Device(name: String, upTime: Long, lastUpdate: Long) {
    override def toString:String = name +" : "+ (System.currentTimeMillis()-lastUpdate) +"\n"
  }

  var devices=scala.collection.mutable.HashMap.empty[String,Device]

  override def receive: Receive = {
    case PropertyChange(topic, payload) => {
      val device: Device = new Device(topic.device, payload.toLong, System.currentTimeMillis())
      devices(topic.device) = device
    }
    case Tick => log.info(" size : {} ={}", devices.size, devices)

  }
}

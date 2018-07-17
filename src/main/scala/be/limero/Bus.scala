package be.limero

import akka.actor.ActorRef
import akka.event.{EventBus, ScanningClassification}
import Bus.PropertyChange

object Bus {

  class BusMessage(topic: Topic)

  case class PropertyChange(topic: Topic, payload: String) extends BusMessage(topic)

  case class Command(topic: Topic, payload: String) extends BusMessage(topic)

  val bus: Bus = new Bus

}

class Bus extends EventBus with ScanningClassification {
  type Event = PropertyChange
  type Classifier = String
  type Subscriber = ActorRef


  // is needed for determining matching classifiers and storing them in an
  // ordered collection
  override protected def compareClassifiers(a: Classifier, b: Classifier): Int = a.compareTo(b)


  // is needed for storing subscribers in an ordered collection
  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int = a.compareTo(b)

  // determines whether a given classifier shall match a given event; it is invoked
  // for each subscription for all received events, hence the name of the classifier
  override protected def matches(classifier: Classifier, event: Event): Boolean = {
 //   println(" compare event "+event+" with classifier "+classifier+ " == "+event.topic.matches(classifier))
    event.topic.matches(classifier)
  }

  // will be invoked for each event for all subscribers which registered themselves
  // for a classifier matching this event
  override protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event
  }
}

package wamp.message

import spray.json.{JsNumber, JsValue}


/**
 * Created by goldv on 6/30/2015.
 */
object Messages {

  val HELLO_TYPE = JsNumber(1)
  val WELCOME_TYPE = JsNumber(2)
  val GOODBYE_TYPE = JsNumber(6)
  val SUBSCRIBE_TYPE = JsNumber(32)
  val SUBSCRIBED_TYPE = JsNumber(33)
  val UNSUBSCRIBE_TYPE = JsNumber(34)
  val UNSUBSCRIBED_TYPE = JsNumber(35)
  val EVENT_TYPE = JsNumber(36)
  val PUBLISH_TYPE = JsNumber(16)

  sealed trait WAMPMessage
  case class Hello(realm: String, details: List[Role]) extends WAMPMessage
  case class Goodbye(reason: String) extends WAMPMessage
  case class Welcome(session: Long, details: List[Role]) extends WAMPMessage

  case class Subscribe(id: Long, topic: String) extends WAMPMessage
  case class Subscribed(id: Long, brokerId: Long) extends WAMPMessage

  case class Unsubscribe(id: Long, subId: Long) extends WAMPMessage
  case class Unsubscribed(id: Long) extends WAMPMessage

  case class Role(name: String, features: Set[String])

  case class Publish(id: Long, topic: String, payload: JsValue) extends WAMPMessage
  case class Event(subId: Long, pubId: Long, payload: JsValue) extends  WAMPMessage
}

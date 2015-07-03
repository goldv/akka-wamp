package org.goldv.wampserver.message

import play.api.libs.json.JsValue

/**
 * Created by goldv on 6/30/2015.
 */
object Messages {

  val HELLO_TYPE = 1
  val WELCOME_TYPE = 2
  val GOODBYE_TYPE = 6
  val SUBSCRIBE_TYPE = 32
  val SUBSCRIBED_TYPE = 33
  val UNSUBSCRIBE_TYPE = 34
  val UNSUBSCRIBED_TYPE = 35
  val EVENT_TYPE = 36
  val PUBLISH_TYPE = 16

  sealed trait WAMPMessage
  case class Hello(realm: String, details: List[Role]) extends WAMPMessage
  case class Goodbye(reason: String) extends WAMPMessage
  case class Welcome(session: Long, details: List[Role]) extends WAMPMessage

  case class Subscribe(id: Long, topic: String) extends WAMPMessage
  case class Subscribed(id: Long, brokerId: Long) extends WAMPMessage

  case class Unsubscribe(id: Long, subId: Long) extends WAMPMessage
  case class Unsubscribed(id: Long) extends WAMPMessage

  case class Role(name: String, features: Set[String])

  case class Publish(id: Long, topic: String, json: JsValue) extends WAMPMessage
  case class Event(id: Long, subId: Long, pubId: Long, payload: JsValue) extends  WAMPMessage
}

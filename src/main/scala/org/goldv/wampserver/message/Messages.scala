package org.goldv.wampserver.message

import play.api.libs.json.JsObject

/**
 * Created by goldv on 6/30/2015.
 */
object Messages {

  val HELLO_TYPE = 1
  val WELCOME_TYPE = 2

  sealed trait WAMPMessage
  case class Hello(realm: String, details: List[Role]) extends WAMPMessage
  case class Welcome(session: Int, details: List[Role]) extends WAMPMessage


  case class Role(name: String, features: Set[String])
}

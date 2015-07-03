package org.goldv.wampserver.protocol

import akka.actor._
import akka.http.scaladsl.model.ws.TextMessage
import org.goldv.wampserver.message.JsonMessageParser
import org.goldv.wampserver.message.Messages.WAMPMessage
import play.api.libs.json.{Json, JsArray}

/**
 * Created by goldv on 7/1/2015.
 */
class JsonSessionActor(sourceActor: ActorRef, subscriptionActor: ActorRef) extends Actor with ActorLogging{

  val parser = new JsonMessageParser
  var protocolActor: ActorRef = _

  override def preStart = {
    protocolActor = context.system.actorOf( ProtocolActor.props(self, subscriptionActor))
  }

  def receive: Receive = {
    case js: JsArray => handleIncoming(js)
    case m:WAMPMessage => handleOutgoing(m)
    case JsonSessionActor.ConnectionClosed => protocolActor ! PoisonPill
  }

  def handleOutgoing(m:WAMPMessage) = {
    val outMessage = parser.write(m).toString()
    sourceActor ! TextMessage( outMessage )
  }

  def handleIncoming(js: JsArray) = parser.parse(js) match{
    case Right(message) => protocolActor ! message
    case Left(err) => log.error(err)
  }
}

object JsonSessionActor{
  case object ConnectionClosed

  def props(sourceActor: ActorRef, subscriptionActor: ActorRef) = Props( new JsonSessionActor(sourceActor, subscriptionActor) )
}

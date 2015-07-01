package org.goldv.wampserver.protocol

import akka.actor.{ActorLogging, Props, ActorRef, Actor}
import akka.http.scaladsl.model.ws.TextMessage
import org.goldv.wampserver.message.JsonMessageParser
import org.goldv.wampserver.message.Messages.WAMPMessage
import play.api.libs.json.{Json, JsArray}

/**
 * Created by goldv on 7/1/2015.
 */
class JsonSessionActor(sourceActor: ActorRef) extends Actor with ActorLogging{

  val parser = new JsonMessageParser
  var protocolActor: ActorRef = _

  override def preStart = {
    protocolActor = context.system.actorOf( WAMPProtocolActor.props(self))
  }

  def receive: Receive = {
    case js: JsArray => parser.parse(js) match{
      case Right(message) => protocolActor ! message
      case Left(err) => log.error(err)
    }
    case JsonSessionActor.ConnectionClosed => log.info(s"connection closed")
    case m:WAMPMessage => sourceActor ! TextMessage( parser.write(m).toString() )
  }
}

object JsonSessionActor{
  case object ConnectionClosed

  def props(sourceActor: ActorRef) = Props( new JsonSessionActor(sourceActor) )
}

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
    protocolActor = context.system.actorOf( WAMPProtocolActor.props(self, subscriptionActor))
  }

  def receive: Receive = {
    case js: JsArray => parser.parse(js) match{
      case Right(message) =>
        log.info(s"-> $message")
        protocolActor ! message
      case Left(err) => log.error(err)
    }
    case JsonSessionActor.ConnectionClosed => protocolActor ! PoisonPill
    case m:WAMPMessage =>
      val outMessage = parser.write(m).toString()
      log.info(s"<- $outMessage")
      sourceActor ! TextMessage( outMessage )
  }
}

object JsonSessionActor{
  case object ConnectionClosed

  def props(sourceActor: ActorRef, subscriptionActor: ActorRef) = Props( new JsonSessionActor(sourceActor, subscriptionActor) )
}

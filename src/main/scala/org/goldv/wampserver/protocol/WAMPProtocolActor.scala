package org.goldv.wampserver.protocol

import akka.actor.{Props, ActorRef, Actor}
import org.goldv.wampserver.message.Messages._
import org.goldv.wampserver.server.WAMPSubscriptionActor
import scala.util.Random

/**
 * Created by goldv on 7/1/2015.
 */
class WAMPProtocolActor(source: ActorRef,  subscriptionActor: ActorRef) extends Actor with akka.actor.ActorLogging {

  var realm: Option[String] = None
  var sessionId: Option[Int] = None

  val rdm = new Random()

  def receive = {
    case h: Hello => handleHello(h)
    case s: Subscribe => handleSubscribe(s)
    case s: Subscribed => handleSubscribed(s)
    case m => log.error(s"received unhandled message $m ignoring")
  }

  override def preStart = subscriptionActor ! WAMPSubscriptionActor.Register( sessionId.getOrElse(0) )

  def handleSubscribed(s: Subscribed) = source ! s

  def handleSubscribe(s: Subscribe) = subscriptionActor ! s

  def handleHello(h: Hello) = if(realm.isEmpty){
    realm = Some(h.realm)
    sessionId = Some(rdm.nextInt())
    log.info(s"creating new session with id $sessionId realm $realm")
    // FIXME session id should be globally unique
    source ! Welcome(sessionId.get, List( Role("broker", Set.empty[String])))
  } else {
    // TODO return error if session already active
  }
}

object WAMPProtocolActor{
  def props(sourceActor: ActorRef, subscriptionActor: ActorRef) = Props( new WAMPProtocolActor(sourceActor, subscriptionActor) )
}
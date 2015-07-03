package org.goldv.wampserver.protocol

import akka.actor.{Props, ActorRef, Actor}
import org.goldv.wampserver.message.Messages._
import org.goldv.wampserver.server.{SubscriptionDispatchActor}
import scala.util.Random

/**
 * Created by goldv on 7/1/2015.
 */

case class UnsubscribeWrapper(brokerId: Long, unsubscribed: Unsubscribed)

class ProtocolActor(source: ActorRef,  subscriptionActor: ActorRef) extends Actor with akka.actor.ActorLogging {

  var realm: String = _
  var sessionId: Int = _

  val rdm = new Random()

  var subscriptionRoutes = Map.empty[Long, ActorRef]

  def receive = {
    case h: Hello => handleHello(h)
  }

  def established: Receive = {
    case e: Event => handleEvent(e)
    case s: Subscribe => handleSubscribe(s)
    case s: Subscribed => handleSubscribed(s)
    case u: UnsubscribeWrapper => handleUnsubscribed(u)
    case m => log.error(s"received unhandled message $m ignoring")
  }

  //override def preStart = subscriptionActor ! WAMPSubscriptionDispatchActor.Register( sessionId.getOrElse(0) )

  def handleSubscribed(s: Subscribed) = {
    subscriptionRoutes = subscriptionRoutes.updated(s.brokerId, sender())
    source ! s
  }

  def handleSubscribe(s: Subscribe) = subscriptionActor ! s

  def handleUnsubscribed(u: UnsubscribeWrapper) = {
    subscriptionRoutes = subscriptionRoutes - u.brokerId
    source ! u.unsubscribed
  }

  def handleEvent(e: Event) = source ! e

  def handleHello(h: Hello) = {
    // FIXME session id should be globally unique
    realm = h.realm
    sessionId = rdm.nextInt()
    log.info(s"creating new session with id $sessionId realm $realm")

    context.become(established)
    source ! Welcome(sessionId, List( Role("broker", Set.empty[String])))
  }
}

object ProtocolActor{
  def props(sourceActor: ActorRef, subscriptionActor: ActorRef) = Props( new ProtocolActor(sourceActor, subscriptionActor) )
}
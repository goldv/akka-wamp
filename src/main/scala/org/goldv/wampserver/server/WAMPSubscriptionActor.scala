package org.goldv.wampserver.server

import akka.actor.{Terminated, ActorRef, ActorLogging, Actor}
import org.goldv.wampserver.message.Messages.{Unsubscribe, Subscribed, Subscribe}

import scala.util.Random

/**
 * Created by goldv on 7/1/2015.
 */
class WAMPSubscriptionActor extends Actor with ActorLogging{
  import WAMPSubscriptionActor._

  val rdm = new Random()

  var subscriptionContainer = SubscriptionContainer()

  def receive = {
    case s:Subscribe => handleSubscribe(s)
    case u:Unsubscribe => handleUnsubscribe(u)
    case r:Register => context.watch(sender())
    case t:Terminated => unsubscribeAll( t.actor )
  }

  def handleUnsubscribe(u: Unsubscribe) = {
    log.info(s"unsub: $u")
    subscriptionContainer = subscriptionContainer.unsubscribe(sender(), u.subId)
    if (subscriptionContainer.countForId(sender(), u.subId) == 0) {
        log.info(s"last unsub for ")
    }
  }

  def handleSubscribe(s: Subscribe) = {
    log.info(s"sub: $s")

    val subscribed = generateSubscribed(s)
    subscriptionContainer = subscriptionContainer.subscribe(sender, subscribed.brokerId, s)

    if(subscriptionContainer.countForTopic(s.topic) == 1){
      // TODO call subscription delegate on client and if successful send back subscribed
      log.info(s"calling subscription delegate for $s")
    }

    sender ! subscribed
  }

  def unsubscribeAll(subscriber: ActorRef) = subscriptionContainer.subscriptionsFor(sender).foreach( id => handleUnsubscribe( Unsubscribe(0, id) ) )

  // FIXME broker id generation needs to be more robust
  def generateSubscribed(s: Subscribe) = Subscribed(s.id, rdm.nextInt())

}

object WAMPSubscriptionActor{
  case class Register(id: Int)
}



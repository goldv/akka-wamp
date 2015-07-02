package org.goldv.wampserver.server

import akka.actor._
import org.goldv.wampserver.message.Messages.{Unsubscribed, Unsubscribe, Subscribed, Subscribe}
import org.goldv.wampserver.protocol.UnsubscribeWrapper
import scala.util.Random
import scala.reflect._

/**
 * Created by goldv on 7/2/2015.
 */
case class SubscribeRegistration(source: ActorRef, subscribe: Subscribe)

class PublisherActor[T](publish: PublisherContainer[T], subscriptionDispatcher: ActorRef) extends Actor with ActorLogging{

  val rdm = new Random()

  var subscriptions = SubscriptionContainer()

  def receive = {
    case event: T =>
    case sr: SubscribeRegistration =>
      val subscribed = handleSubscribe(sr)
      context.watch(sr.source)
      sr.source ! subscribed
    case u: Unsubscribe =>
      handleUnsubscribe(sender(), u.subId)
      sender() ! UnsubscribeWrapper( u.subId, Unsubscribed(u.id) )
    case t:Terminated => {
      log.info(s"protocol actor terminated")
      subscriptions.unsubscribesFor(t.actor).foreach( u => handleUnsubscribe(t.actor, u.subId ) )
    }
  }

  def handleSubscribe(sr: SubscribeRegistration): Subscribed = {
    val subscribed = generateSubscribed(sr.subscribe)
    subscriptions = subscriptions.subscribe(subscribed.brokerId, sr)
    if (subscriptions.countForTopic(sr.subscribe.topic) == 1) {
      log.info(s"sub base topic: ${publish.publisher.baseTopic} topic: ${sr.subscribe.topic}")
    }

    subscribed
  }

  def handleUnsubscribe(source: ActorRef, subId: Long) = {
    for {
      topic <- subscriptions.topicFor(source, subId)
    } {
      subscriptions = subscriptions.unsubscribe(source, subId)
      if(subscriptions.countForId(sender(), subId) == 0) {
        log.info(s"unsub base topic:  ${publish.publisher.baseTopic} topic: $topic")
      }
    }
  }

  def buildWAMPSubscription(source: ActorRef, _topic: String) = new WAMPSubscription[T] {
    def topic: String = _topic
    def publish(event: T) = {

    }
    def error(reason: String) = {}
  }

  override def preStart() = {
    subscriptionDispatcher ! DispatchRegistration(publish.publisher.baseTopic, self)
  }

  // FIXME broker id generation needs to be more robust
  def generateSubscribed(s: Subscribe) = Subscribed(s.id, rdm.nextInt())

}

object PublisherActor{
  def apply[T](publish: PublisherContainer[T], subscriptionDispatchActor: ActorRef) = Props( new PublisherActor(publish, subscriptionDispatchActor) )
}

class SubscriptionContainer(actorSubs: Map[ActorRef, Map[Long, Subscribe]], topicSubs: Map[String, Map[Long, SubscribeRegistration]]){

  def subscribe(id: Long, s: SubscribeRegistration): SubscriptionContainer = {
    val idToSub = actorSubs.getOrElse(s.source, Map.empty[Long, Subscribe])
    val subMap = topicSubs.getOrElse(s.subscribe.topic, Map.empty[Long, SubscribeRegistration]) + (id -> s)
    SubscriptionContainer(actorSubs.updated( s.source, idToSub.updated(id, s.subscribe) ), topicSubs.updated(s.subscribe.topic, subMap) )
  }

  def unsubscribe(sender: ActorRef, id: Long): SubscriptionContainer = deleteSub(sender, id).getOrElse(this)

  def subscriptionsFor(sender: ActorRef) = actorSubs.get(sender).map(_.keys.toList).getOrElse(Nil)

  def unsubscribesFor(sender: ActorRef) = {
    val unsubscribes = for{
      subs <- actorSubs.get(sender)
    } yield subs.map{ case (id, sub) => Unsubscribe(0, id) }

    unsubscribes.getOrElse(Nil)
  }

  def countForTopic(topic: String): Int = topicSubs.get(topic).map(_.size).getOrElse(0)

  def countForId(sender: ActorRef, id: Long): Int = {
    val count = for{
      topic <- topicFor(sender, id)
      subsForTopic <- topicSubs.get(topic)
    } yield subsForTopic.size

    count.getOrElse(0)
  }

  def topicFor(sender: ActorRef, id: Long) = for {
    idToSub <- actorSubs.get(sender)
    topic <- idToSub.get(id).map(_.topic)
  } yield topic

  private def deleteSub(sender: ActorRef, id: Long) = for {
    idToSub <- actorSubs.get(sender)
    sub <- idToSub.get(id)
    topicMap <- topicSubs.get(sub.topic)
  } yield {
    SubscriptionContainer(actorSubs.updated(sender,idToSub - id), topicSubs.updated(sub.topic, topicMap - id) )
  }

}

object SubscriptionContainer{
  def apply() = new SubscriptionContainer(Map.empty[ActorRef,Map[Long, Subscribe]], Map.empty[String, Map[Long, SubscribeRegistration]])
  def apply(actorSubs: Map[ActorRef, Map[Long, Subscribe]], topicSubs: Map[String, Map[Long, SubscribeRegistration]]) = new SubscriptionContainer(actorSubs, topicSubs)
}
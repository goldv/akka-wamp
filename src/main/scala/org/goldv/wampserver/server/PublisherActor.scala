package org.goldv.wampserver.server

import akka.actor._
import akka.util.Timeout
import com.fasterxml.jackson.databind.JsonNode
import org.goldv.wampserver.message.Messages._
import org.goldv.wampserver.protocol.UnsubscribeWrapper
import play.api.libs.json.{JsValue, Json}
import akka.pattern.ask
import scala.concurrent.Future
import scala.util.Random
import scala.concurrent._
import scala.concurrent.duration._
import play.api.libs.json.Writes.JsonNodeWrites

/**
 * Created by goldv on 7/2/2015.
 */
case class SubscribeRegistration(source: ActorRef, subscribe: Subscribe)
case class SubscribedRegistration(subscribe: SubscribeRegistration, subscribed: Subscribed, publisher: ActorRef)

class PublisherActor[T](publish: PublisherContainer, subscriptionDispatcher: ActorRef) extends Actor with ActorLogging with WAMPConfiguration{

  val rdm = new Random()

  var subscriptions = SubscriptionContainer()

  def receive = {
    case p: Publish =>
      handlePublish(p)
    case sr: SubscribeRegistration =>
      val subscribed = handleSubscribe(sr)
      context.watch(sr.source)
      notifySubscriber( SubscribedRegistration(sr, subscribed, self) )
    case sr: SubscribedRegistration =>
      subscriptions.subscribed(sr.subscribed.brokerId)
      sender() ! publish.newSubscription(self, sr.subscribe.subscribe.topic) // send subscription back to client api
      sr.subscribe.source ! sr.subscribed // confirm the subscription to the subscriber
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
      val subscription = publish.newSubscription(self, sr.subscribe.topic)
      log.info(s"sub base topic: ${publish.publisher.baseTopic} topic: ${sr.subscribe.topic}")
    }

    subscribed
  }

  def notifySubscriber(sr: SubscribedRegistration): Future[Unit] = Future{
    val subscriber = publish.newSubscriber(sr)
    publish.publisher.onSubscribe(subscriber)
  }(apiExecutionContext)

  def handleUnsubscribe(source: ActorRef, subId: Long) = subscriptions.topicFor(source, subId).foreach{ topic =>
    subscriptions = subscriptions.unsubscribe(source, subId)
    if(subscriptions.countForId(sender(), subId) == 0) {
      log.info(s"unsub base topic:  ${publish.publisher.baseTopic} topic: $topic")
    }
  }

  def handlePublish(p: Publish) = {

    // TODO cache this for future subscribers
    val payload = Json.obj(
      "dataType" -> p.event.dataType,
      "key" -> p.event.id,
      "data" -> Json.toJson( p.event.data )
    )

    subscriptions.subscribersForTopic(p.topic).foreach{ case (subId, sr) =>
      sr.source ! generateEvent(subId, p, payload)
    }
  }

  override def preStart() = {
    subscriptionDispatcher ! DispatchRegistration(publish.publisher.baseTopic, self)
  }

  def generateEvent(subId: Long, p: Publish, payload: JsValue) = Event(rdm.nextInt(), subId, p.id, payload)

  // FIXME broker id generation needs to be more robust
  def generateSubscribed(s: Subscribe) = Subscribed(s.id, rdm.nextInt())

}

object PublisherActor{
  def apply(publish: PublisherContainer, subscriptionDispatchActor: ActorRef) = Props( new PublisherActor(publish, subscriptionDispatchActor) )
}

case class PublisherContainer(publisher: WAMPPublisher){

  val rdm = new Random()
  implicit val timeout = new Timeout(5 seconds)

  def newSubscriber(sr: SubscribedRegistration) = new WAMPSubscriber{
    def topic = sr.subscribe.subscribe.topic
    def subscribed = Await.result( (sr.publisher ? sr).mapTo[WAMPSubscription], 2 seconds) // FIXME better to return the future
    def error(reason: String) = ()
  }

  def newSubscription(source: ActorRef, _topic: String) = new WAMPSubscription{
    def topic = _topic
    def publish(event: PublicationEvent) = source ! Publish(rdm.nextLong(), _topic, event)

    def error(reason: String) = {

    }
  }
}

class SubscriptionContainer(actorSubs: Map[ActorRef, Map[Long, Subscribe]], topicSubs: Map[String, Map[Long, SubscribeRegistration]], subscribedSet: Set[Long]){

  def subscribe(id: Long, s: SubscribeRegistration): SubscriptionContainer = {
    val idToSub = actorSubs.getOrElse(s.source, Map.empty[Long, Subscribe])
    val subMap = topicSubs.getOrElse(s.subscribe.topic, Map.empty[Long, SubscribeRegistration]) + (id -> s)
    SubscriptionContainer(actorSubs.updated( s.source, idToSub.updated(id, s.subscribe) ), topicSubs.updated(s.subscribe.topic, subMap), subscribedSet )
  }

  def subscribed(id: Long) = SubscriptionContainer(actorSubs, topicSubs, subscribedSet + id)

  def unsubscribe(sender: ActorRef, id: Long): SubscriptionContainer = deleteSub(sender, id).getOrElse(this)

  def subscriptionsFor(sender: ActorRef) = actorSubs.get(sender).map(_.keys.toList).getOrElse(Nil)

  def subscribersForTopic(topic: String) = topicSubs.getOrElse(topic, Map.empty[Long, SubscribeRegistration])

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
    SubscriptionContainer(actorSubs.updated(sender,idToSub - id), topicSubs.updated(sub.topic, topicMap - id), subscribedSet - id )
  }

}

object SubscriptionContainer{
  def apply() = new SubscriptionContainer(Map.empty[ActorRef,Map[Long, Subscribe]], Map.empty[String, Map[Long, SubscribeRegistration]], Set.empty[Long])
  def apply(actorSubs: Map[ActorRef, Map[Long, Subscribe]], topicSubs: Map[String, Map[Long, SubscribeRegistration]], subscribed: Set[Long]) = new SubscriptionContainer(actorSubs, topicSubs, subscribed)
}
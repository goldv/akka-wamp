package org.goldv.wampserver.server

import akka.actor._
import akka.util.Timeout
import org.goldv.wampserver.message.Messages._
import org.goldv.wampserver.protocol.UnsubscribeWrapper
import play.api.libs.json.{JsObject, JsValue, Json}
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

  var publicationCache = Map.empty[(String, String), JsObject]

  var subscriptions = SubscriptionContainer()

  def receive = {
    case p: Publish =>
      handlePublish(p)
    case sr: SubscribeRegistration =>
      context.watch(sr.source)
      handleSubscribe(sr)
    case sr: SubscribedRegistration =>
      subscriptions.subscribed(sr.subscribed.brokerId)
      sender() ! publish.newSubscription(self, sr.subscribe.subscribe.topic) // send subscription back to client api
      sr.subscribe.source ! sr.subscribed // confirm the subscription to the subscriber
      publicationCache.values.foreach( payload => sr.subscribe.source ! generateEvent(sr.subscribed.brokerId, rdm.nextLong(), payload )) // notify with cached values
    case u: Unsubscribe =>
      handleUnsubscribe(sender(), u.subId)
      sender() ! UnsubscribeWrapper( u.subId, Unsubscribed(u.id) )
    case t:Terminated => {
      log.info(s"protocol actor terminated")
      subscriptions.unsubscribesFor(t.actor).foreach( u => handleUnsubscribe(t.actor, u.subId ) )
    }
  }

  def handleSubscribe(sr: SubscribeRegistration) = {
    val subscribed = generateSubscribed(sr.subscribe)
    subscriptions = subscriptions.subscribe(subscribed.brokerId, sr)
    if (subscriptions.count == 1) {
      // first subscription, request from data source
      notifySubscriber(SubscribedRegistration(sr, subscribed, self))
    } else {
      // already subscribed to data source, return subscribed and publish cache contents
      sr.source ! subscribed
      publicationCache.values.foreach( payload => sr.source ! generateEvent(subscribed.brokerId, rdm.nextLong(), payload ))
    }
  }

  def notifySubscriber(sr: SubscribedRegistration): Future[Unit] = Future{
    val subscriber = publish.newSubscriber(sr)
    publish.publisher.onSubscribe(subscriber)
  }(apiExecutionContext)

  def handleUnsubscribe(source: ActorRef, subId: Long) = {
    subscriptions = subscriptions.unsubscribe(source, subId)
    if(subscriptions.count == 0) {
      // TODO notify subscriber
      log.info(s"unsub topic:  ${publish.publisher.topic}")
    }
  }

  def handlePublish(p: Publish) = {

    val payload = buildPayload(p.event)
    val cacheKey = (p.event.dataType, p.event.id)

    if(p.event.isFinal){
      publicationCache = publicationCache - cacheKey
    } else {
      // cache for future subscribers to receive first update
      publicationCache = publicationCache.updated( cacheKey, payload )
    }

    subscriptions.subscribers.foreach{ case (subId, sr) =>
      sr.source ! generateEvent(subId, p.id, payload)
    }
  }

  override def preStart() = {
    subscriptionDispatcher ! DispatchRegistration(publish.publisher.topic, self)
  }

  def generateEvent(subId: Long, pubId: Long, payload: JsValue) = Event(rdm.nextInt(), subId, pubId, payload)

  def buildPayload(event: PublicationEvent) = Json.obj(
    "dataType" -> event.dataType,
    "key" -> event.id,
    "isFinal" -> event.isFinal,
    "data" -> Json.toJson( event.data )
  )

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
      // TODO
    }
  }
}

class SubscriptionContainer(actorSubs: Map[ActorRef, Map[Long, Subscribe]], subs: Map[Long, SubscribeRegistration]){

  def subscribed(id: Long) = SubscriptionContainer(actorSubs, subs )

  def unsubscribe(sender: ActorRef, id: Long): SubscriptionContainer = deleteSub(sender, id).getOrElse(this)

  def subscriptionsFor(sender: ActorRef) = actorSubs.get(sender).map(_.keys.toList).getOrElse(Nil)

  def subscribers = subs

  def count: Int = subs.size

  def unsubscribesFor(sender: ActorRef) = {
    val unsubscribes = for{
      subs <- actorSubs.get(sender)
    } yield subs.map{ case (id, sub) => Unsubscribe(0, id) }

    unsubscribes.getOrElse(Nil)
  }

  def subscribe(id: Long, s: SubscribeRegistration): SubscriptionContainer = {
    val idToSub = actorSubs.getOrElse(s.source, Map.empty[Long, Subscribe])
    SubscriptionContainer(actorSubs.updated( s.source, idToSub.updated(id, s.subscribe) ), subs.updated(id, s) )
  }

  private def deleteSub(sender: ActorRef, id: Long) = for {
    idToSub <- actorSubs.get(sender)
    sub <- idToSub.get(id)
  } yield {
    SubscriptionContainer(actorSubs.updated(sender,idToSub - id), subs - id )
  }
}

object SubscriptionContainer{
  def apply() = new SubscriptionContainer(Map.empty[ActorRef,Map[Long, Subscribe]], Map.empty[Long, SubscribeRegistration])
  def apply(actorSubs: Map[ActorRef, Map[Long, Subscribe]], subs: Map[Long, SubscribeRegistration]) = new SubscriptionContainer(actorSubs, subs)
}
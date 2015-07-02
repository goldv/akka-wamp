package org.goldv.wampserver.server

import akka.actor.ActorRef
import org.goldv.wampserver.message.Messages.{Unsubscribe, Subscribe}

/**
 * Created by goldv on 7/2/2015.
 */
class SubscriptionContainer(topicToSubId:  Map[String, Set[Long]], subscribers: Map[ActorRef, Map[Long, Subscribe]]) {

  def subscribe(sender: ActorRef, id: Long, s: Subscribe): SubscriptionContainer = {
    val idToSub = subscribers.getOrElse(sender, Map.empty[Long, Subscribe])
    val subSet = topicToSubId.getOrElse(s.topic, Set.empty[Long]) + id
    SubscriptionContainer(topicToSubId.updated(s.topic, subSet), subscribers.updated( sender, idToSub.updated(id, s) ) )
  }

  def unsubscribe(sender: ActorRef, id: Long): SubscriptionContainer = deleteSub(sender, id).getOrElse(this)

  def subscriptionsFor(sender: ActorRef) = subscribers.get(sender).map(_.keys.toList).getOrElse(Nil)

  def countForTopic(topic: String): Int = topicToSubId.get(topic).map(_.size).getOrElse(0)

  def countForId(sender: ActorRef, id: Long): Int = {
    val count = for{
      idToSub <- subscribers.get(sender)
      topic <- idToSub.get(id).map(_.topic)
      subsForTopic <- topicToSubId.get(topic)
    } yield subsForTopic.size

    count.getOrElse(0)
  }

  private def deleteSub(sender: ActorRef, id: Long) = for {
    idToSub <- subscribers.get(sender)
    sub <- idToSub.get(id)
    topicSet <- topicToSubId.get(sub.topic)
  } yield {
    SubscriptionContainer(topicToSubId.updated(sub.topic, topicSet - id), subscribers.updated(sender,idToSub - id) )
  }

}


object SubscriptionContainer{
  def apply() = new SubscriptionContainer(Map.empty[String, Set[Long]], Map.empty[ActorRef, Map[Long, Subscribe]])
  def apply(topicToSubId:  Map[String, Set[Long]], subscribers: Map[ActorRef, Map[Long, Subscribe]]) = new SubscriptionContainer(topicToSubId, subscribers)
}

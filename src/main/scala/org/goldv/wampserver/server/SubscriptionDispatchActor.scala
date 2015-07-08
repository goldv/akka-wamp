package org.goldv.wampserver.server

import akka.actor.{Actor, ActorLogging, ActorRef}
import org.goldv.wampserver.message.Messages.Subscribe
import scala.annotation.tailrec

/**
 * Created by goldv on 7/1/2015.
 */
case class DispatchRegistration(baseTopic: String, publisher: ActorRef)

class SubscriptionDispatchActor extends Actor with ActorLogging{

  import TopicUtils._

  var publishers = Map.empty[String, ActorRef]

  def receive = {
    case dr: DispatchRegistration => publishers = publishers.updated(dr.baseTopic, dr.publisher)
    case s: Subscribe => publishers.get(s.topic).foreach( _ ! SubscribeRegistration(sender(), s) )
  }

  def publisherForTopic(topic: String) = publisherForTopics( baseTopicsFromTopic(topic) )

  @tailrec
  final def publisherForTopics(topics: List[String]): Option[ActorRef] = topics match{
    case Nil => None
    case base :: Nil => publishers.get(base)
    case base :: tail => publishers.get(base) match{
      case Some(b) => Some(b)
      case None => publisherForTopics(tail)
    }
  }
}

object TopicUtils{

  val TOPIC_DELIM = '.'

  def baseTopicsFromTopic(topic: String): List[String] = {
    val subTopics = topic.split(TOPIC_DELIM).toList
    if(subTopics.size > 1){
      subTopics.drop(1).foldRight( subTopics.take(1) )((item, acc) => acc :+ s"${acc.head}.$item" )
    } else List(topic)
  }

}





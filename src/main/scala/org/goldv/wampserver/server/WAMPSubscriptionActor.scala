package org.goldv.wampserver.server

import akka.actor.{ActorLogging, Actor}
import org.goldv.wampserver.message.Messages.{Subscribed, Subscribe}

import scala.util.Random

/**
 * Created by goldv on 7/1/2015.
 */
class WAMPSubscriptionActor extends Actor with ActorLogging{

  val rdm = new Random()

  var topicToSubscribe = Map.empty[String, Map[Long, Subscribed]]

  def receive = {
    case s:Subscribe => handleSubscribe(s)
  }

  def handleSubscribe(s: Subscribe) = {
    log.info(s"subscription actor received $s")
    val first = !topicToSubscribe.contains(s.topic)

    val subscribed = generateSubscribed(s)
    val subscribedMap = topicToSubscribe.getOrElse(s.topic, Map.empty[Long, Subscribed]).updated(s.subscriptionId, subscribed )
    topicToSubscribe = topicToSubscribe.updated(s.topic, subscribedMap)

    if(first){
      // TODO call subscription delegate on client and if successful send back subscribed
      log.info(s"calling subscription delegate for $s")
    }

    sender ! subscribed
  }

  // FIXME broker id generation needs to be more robust
  def generateSubscribed(s: Subscribe) = Subscribed(s.subscriptionId, rdm.nextInt())

}



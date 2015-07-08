package org.goldv.wampserver.server

import com.fasterxml.jackson.databind.JsonNode

/**
 * Created by goldv on 7/2/2015.
 */
class PublicationEvent(val id: String, val dataType: String, val data: JsonNode)

trait WAMPSubscriber{
  def topic: String
  def subscribed: WAMPSubscription
  def error(reason: String): Unit
}

trait WAMPSubscription{
  def topic: String
  def publish(event: PublicationEvent)
  def error(reason: String)
}

trait WAMPPublisher{
  def baseTopic: String
  def onSubscribe(sub: WAMPSubscriber)
  def onUnsubscribe(topic: String)
}

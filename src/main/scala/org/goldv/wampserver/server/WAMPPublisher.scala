package org.goldv.wampserver.server

import scala.concurrent.Future

/**
 * Created by goldv on 7/2/2015.
 */

trait WAMPSubscriber[T]{
  def topic: String
  def subscribed: Future[WAMPSubscription[T]]
  def error(reason: String): Future[Unit]
}

trait WAMPSubscription[T]{
  def topic: String
  def publish(event: T)
  def error(reason: String)
}

trait WAMPPublisher[T] {
  def baseTopic: String
  def onSubscribe(sub: WAMPSubscriber[T])
  def onUnsubscribe(topic: String)
}

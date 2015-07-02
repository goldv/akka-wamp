package org.goldv.wampserver.server

/**
 * Created by goldv on 7/2/2015.
 */

trait WAMPSubscription[T]{
  def topic: String
  def publish(event: T)
  def error(reason: String)
}

trait WAMPPublisher[T] {
  def baseTopic: String
  def onSubscribe(sub: WAMPSubscription[T])
  def onUnsubscribe(topic: String)
}

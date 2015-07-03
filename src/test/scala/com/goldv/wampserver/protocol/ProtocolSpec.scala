package com.goldv.wampserver.protocol

import akka.actor.ActorSystem
import akka.testkit._
import org.goldv.wampserver.message.Messages.{Welcome, Hello}
import org.goldv.wampserver.protocol.ProtocolActor
import org.scalatest.{MustMatchers, WordSpecLike}


/**
 * Created by goldv on 7/3/2015.
 */
class ProtocolSpec extends TestKit(ActorSystem("ProtocolSpec")) with ImplicitSender with MustMatchers with WordSpecLike{

  "protocol actor" must{

    "receive welcome message" in{
      val sourceActor = TestProbe()
      val subscriptionActor = TestProbe()

      val protocolActor = system.actorOf(ProtocolActor.props(sourceActor.ref, subscriptionActor.ref))
      protocolActor ! Hello("realm", Nil)

      sourceActor.expectMsgClass( classOf[Welcome] )
    }
  }

}

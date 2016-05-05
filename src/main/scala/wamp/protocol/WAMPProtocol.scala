package wamp.protocol

import akka.NotUsed
import akka.event.{SubchannelClassification, EventBus}
import akka.http.scaladsl.model.ws.{TextMessage, Message}
import akka.stream.BidiShape
import akka.util.Subclassification
import spray.json.{JsArray, JsonParser}
import wamp.message.{Messages, MessageParser}
import Messages._
import wamp.message.{Messages, MessageParser}
import MessageParser.{write, parse}
import akka.stream.scaladsl.GraphDSL.Implicits._
import akka.stream.scaladsl._
import wamp.message.Messages._
import wamp.util.TopicUtils
import scala.util.{Random, Success, Failure, Try}

object WAMPProtocol {

  def handler(id: Long) = serialization
    .atop(WAMPSession.session(id))
    .atop(WAMPSubscription.subscription)

  def readMessage(msg: Message): Try[WAMPMessage] = msg match{
    case tm:TextMessage.Strict => parseArray(tm.text)
    case _ => Failure(new Exception("Unable to parse binary message"))
  }

  def writeMessage(msg: WAMPMessage): Message = TextMessage( write(msg).toString() )

  def parseArray(msg: String) = Try(JsonParser(msg)) match{
    case Success(a:JsArray) => parse(a)
    case _ => Failure(new Exception(s"Unable to parse $msg to array"))
  }

  def serialization: BidiFlow[Message, WAMPMessage, WAMPMessage, Message, NotUsed] = {
    val read = Flow[Message]
      .map(readMessage)
      .mapConcat {
      case Success(msg) => msg :: Nil
      case Failure(cause) => Nil
    }

    val write = Flow[WAMPMessage].map(writeMessage)

    // create BidiFlow with these to separated flows
    BidiFlow.fromFlowsMat(read, write)((m1, m2) => NotUsed)
  }
}

object WAMPSession {

  def session(id: Long): BidiFlow[WAMPMessage, WAMPMessage, WAMPMessage, WAMPMessage, NotUsed] = {

    // create a new BidiFlow from a stream graph
    BidiFlow.fromGraph(GraphDSL.create() { implicit builder =>

      // create the session, currently just filtering invalid messages
      val sessionStateFlow = builder.add(Flow[WAMPMessage].statefulMapConcat(sessionFlow(id)))

      // partition welcome message to be returned to sender
      val welcomePartition = builder.add(Partition[WAMPMessage](2, {
        case w: Welcome => 0
        case _ => 1
      }))

      // partition the output of stateful session stage
      sessionStateFlow.out ~> welcomePartition.in

      // merge for incoming and welcome
      val merge = builder.add(Merge[WAMPMessage](2))

      welcomePartition.out(0) ~> merge.in(1)

      BidiShape(sessionStateFlow.in, welcomePartition.out(1), merge.in(0), merge.out)
    })
  }

  def sessionFlow(id: Long) = () => {

    var sessionActive = false

    def handleHello(h: Hello) = {
      if(!sessionActive){
        sessionActive = true
        List(Welcome(id, List( Role("broker", Set.empty[String]))))
      } else List.empty[WAMPMessage]
    }

    msg: WAMPMessage => msg match{
      case h:Hello => handleHello(h)
      case m => List(m)
    }
  }
}

object WAMPSubscription{

  def subscription: BidiFlow[WAMPMessage, WAMPMessage, WAMPMessage, WAMPMessage, NotUsed] = {

    // create a new BidiFlow from a stream graph
    BidiFlow.fromGraph(GraphDSL.create() { implicit builder =>

      // create the session, currently just filtering invalid messages
      val subscriptionStateFlow = builder.add(Flow[WAMPMessage].statefulMapConcat(subscriptionFlow))

      // partition welcome message to be returned to sender
      val subscriptionPartition = builder.add(Partition[WAMPMessage](2, partitionMessage))

      // merge for incoming and welcome
      val merge = builder.add(Merge[WAMPMessage](2))

      // partition the subscription state flow
      subscriptionStateFlow.out ~> subscriptionPartition.in

      merge.out ~> subscriptionStateFlow.in

      BidiShape(merge.in(1), subscriptionPartition.out(1), merge.in(0), subscriptionPartition.out(0) )
    })
  }

  def partitionMessage(msg: WAMPMessage) = msg match{
    case s: Subscribed => 0
    case e: Event => 0
    case _ => 1
  }

  def subscriptionFlow = () => {

    var subscriptions = Map.empty[String, Subscribed]
    var lastPublicationId = 0L
    val rdm = Random

    def handleSubscribe(s: Subscribe) = {
      if(!subscriptions.contains(s.topic)){
        val subscribed = Subscribed(s.id, Math.abs(rdm.nextInt()))
        subscriptions = subscriptions.updated(s.topic,subscribed)
        List(s, subscribed)
      } else List.empty[WAMPMessage]
    }

    def handlePublish(p: Publish) = {
      lastPublicationId += 1
      TopicUtils.wildcardTopicsFromTopic(p.topic)
        .filter(subscriptions.contains)
        .map( topic => Event(subscriptions(topic).brokerId, lastPublicationId, p.payload))
    }

    msg: WAMPMessage => msg match{
      case s:Subscribe => handleSubscribe(s)
      case p:Publish => handlePublish(p)
      case m => Nil
    }
  }
}





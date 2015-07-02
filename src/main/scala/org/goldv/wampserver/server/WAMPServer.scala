package org.goldv.wampserver.server

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebsocket}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExpectedWebsocketRequestRejection, Route}
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import org.goldv.wampserver.protocol.JsonSessionActor
import org.goldv.wampserver.protocol.JsonSessionActor.ConnectionClosed
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsArray, JsSuccess, Json}

/**
 * Created by goldv on 7/1/2015.
 */
case class PublisherContainer[T](publisher: WAMPPublisher[T], writer: play.api.libs.json.Writes[T]){
  def newSubscription(source: ActorRef, topic: String) = {

  }
}

class WAMPServer(host: String, port: Int, wsPath: String, route: Option[Route] = None, publishers: List[PublisherContainer[_]]) {

  val log = LoggerFactory.getLogger(classOf[WAMPServer])

  def register[T](publisher: WAMPPublisher[T], writer : play.api.libs.json.Writes[T]) = WAMPServer(host, port, wsPath, route, PublisherContainer[T](publisher, writer) :: publishers)

  def bind() = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    import system.dispatcher

    // start the subscription dispatch actor, serves as a stateless router for subscription requests
    val subscriptionActor = system.actorOf(Props[SubscriptionDispatchActor])

    // start actor for each registered publisher
    publishers.foreach( p => system.actorOf( PublisherActor(p, subscriptionActor)))

    val wsRoute =  path(wsPath) {
      get {
        (handleWebsocketMessagesWithSyncSource _).tupled(createSinkSource(subscriptionActor))
      }
    }

    val finalRoute = route.foldRight(wsRoute)( _ ~ _ )
    Http().bindAndHandle(finalRoute, host, port).map{ serverBinding =>
      () => serverBinding.unbind().onComplete( _ => system.shutdown() )
    }
  }

  def createSinkSource(subscriptionActor: ActorRef)(implicit system: ActorSystem, materializer: ActorMaterializer) = {
    val (sourceActor, publisher) =  Source.actorRef[Message](1, OverflowStrategy.fail).toMat(Sink.publisher)(Keep.both).run()
    val outSource = Source(publisher)

    val sessionActor = system.actorOf( JsonSessionActor.props(sourceActor, subscriptionActor))

    val sink =  Flow[Message]
      .collect{ case TextMessage.Strict(msg) => Json.fromJson[JsArray](Json.parse(msg)) }
      .collect{ case JsSuccess(sub, _) => sub }
      .to( Sink.actorRef[JsArray](sessionActor , ConnectionClosed) )

    (sink, outSource)
  }

  def handleWebsocketMessagesWithSyncSource(sink: Sink[Message, Any], source: Source[Message, Any]): Route =
    optionalHeaderValueByType[UpgradeToWebsocket](){
      case Some(upgrade) => complete(upgrade.handleMessagesWithSinkSource(sink, source, upgrade.requestedProtocols.headOption))
      case None => reject(ExpectedWebsocketRequestRejection)
    }

}

object WAMPServer{
  def apply(host: String, port: Int, wsPath: String, route: Option[Route] = None, publishers: List[PublisherContainer[_]] = Nil) = new WAMPServer(host, port, wsPath, route, publishers)
}

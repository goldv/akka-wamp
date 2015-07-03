package org.goldv.wampserver.server

import java.util.concurrent.Executors

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage, UpgradeToWebsocket}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExpectedWebsocketRequestRejection, Route}
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.typesafe.config.Config
import org.goldv.wampserver.message.Messages.Publish
import org.goldv.wampserver.protocol.JsonSessionActor
import org.goldv.wampserver.protocol.JsonSessionActor.ConnectionClosed
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsArray, JsSuccess, Json}

import scala.concurrent.ExecutionContext
import scala.util.Random

/**
 * Created by goldv on 7/1/2015.
 */


class WAMPServer(host: String, port: Int, wsPath: String, route: Option[Route] = None, publishers: List[PublisherContainer[_]], config: Config) {

  val log = LoggerFactory.getLogger(classOf[WAMPServer])

  def register[T](publisher: WAMPPublisher[T], writer : play.api.libs.json.Writes[T]) = WAMPServer(host, port, wsPath, route, PublisherContainer[T](publisher, writer) :: publishers, config)

  def bind() = {
    implicit val system = ActorSystem("wamp", config)
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
    val (sourceActor, publisher) =  Source.actorRef[Message](100, OverflowStrategy.dropTail).toMat(Sink.publisher)(Keep.both).run()
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
      case None => {
        println("rejecting")
        reject(ExpectedWebsocketRequestRejection)
      }
    }

}

object WAMPServer{

  def apply(host: String, port: Int, wsPath: String, route: Option[Route] = None, publishers: List[PublisherContainer[_]] = Nil, config: Config) = new WAMPServer(host, port, wsPath, route, publishers, config)
}

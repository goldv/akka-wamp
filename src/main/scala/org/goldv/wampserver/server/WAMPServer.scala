package org.goldv.wampserver.server

import java.util.logging.Logger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{TextMessage, UpgradeToWebsocket, Message}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExpectedWebsocketRequestRejection, Route}
import akka.stream.{OverflowStrategy, ActorMaterializer}
import akka.stream.scaladsl._
import org.goldv.wampserver.protocol.JsonSessionActor
import org.goldv.wampserver.protocol.JsonSessionActor.ConnectionClosed
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsSuccess, JsArray, Json}

/**
 * Created by goldv on 7/1/2015.
 */
class WAMPServer(host: String, port: Int, wsPath: String, route: Option[Route] = None) {

  val log = LoggerFactory.getLogger(classOf[WAMPServer])

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val wsRoute =  path(wsPath) {
    get {
      (handleWebsocketMessagesWithSyncSource _).tupled(createSinkSource)
    }
  }

  def bind() = {
    val finalRoute = route.foldRight(wsRoute)( _ ~ _ )
    Http().bindAndHandle(finalRoute, host, port).map{ serverBinding =>
      () => serverBinding.unbind().onComplete( _ => system.shutdown() )
    }
  }

  def createSinkSource = {
    val (sourceActor, publisher) =  Source.actorRef[Message](1, OverflowStrategy.fail).toMat(Sink.publisher)(Keep.both).run()
    val outSource = Source(publisher)

    val sessionActor = system.actorOf( JsonSessionActor.props(sourceActor))

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

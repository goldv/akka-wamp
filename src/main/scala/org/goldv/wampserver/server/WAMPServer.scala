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
class WAMPServer(host: String, port: Int, wsPath: String, publishers: List[PublisherContainer], rootResource: String, directoryResources: List[(String, String)]) {

  def this(host: String, port: Int, wsPath: String, root: String) = this(host, port, wsPath, Nil, root, Nil)

  val log = LoggerFactory.getLogger(classOf[WAMPServer])

  def register[T](publisher: WAMPPublisher) = new WAMPServer(host, port, wsPath, PublisherContainer(publisher) :: publishers, rootResource, directoryResources )
  def withRootResource(fileName: String) = new WAMPServer(host, port, wsPath, publishers, rootResource, directoryResources )
  def withDirResource(path: String, dirName: String) = new WAMPServer(host, port, wsPath, publishers, rootResource, path -> dirName :: directoryResources )

  val rootRoute = pathSingleSlash {
    getFromResource(rootResource)
  }

  val dirRoutes = directoryResources.map{ case (prefix, dir) =>
    pathPrefix(prefix) {
      getFromResourceDirectory(dir)
    }
  }

  def bind() = {
    implicit val system = ActorSystem("wamp")
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

    val finalRoute = dirRoutes.foldRight(wsRoute)(_ ~ _) ~ rootRoute
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

  def handleWebsocketMessagesWithSyncSource(sink: Sink[Message, Any], source: Source[Message, Any]): Route = {
    optionalHeaderValueByType[UpgradeToWebsocket]() {
      case Some(upgrade) => complete(upgrade.handleMessagesWithSinkSource(sink, source, upgrade.requestedProtocols.headOption))
      case None => reject(ExpectedWebsocketRequestRejection)
    }
  }

}



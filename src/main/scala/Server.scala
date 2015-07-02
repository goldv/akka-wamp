import akka.http.scaladsl.server.Directives
import org.goldv.wampserver.server.{PublisherContainer, WAMPSubscription, WAMPPublisher, WAMPServer}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends App{

  import Directives._

  val route = get {
    pathPrefix("js") {
      getFromResourceDirectory("js")
    } ~
    pathSingleSlash {
      get {
        getFromResource("index.html")
      }
    }
  }

  case class Test(is: String, data: Int)
  val testWrites = Json.writes[Test]

  val publisher = new WAMPPublisher[Test] {
    def baseTopic = "com.myapp"
    def onSubscribe(sub: WAMPSubscription[Test]) = {
      println(s"subscription received ${sub.topic}")
    }
    def onUnsubscribe(topic: String) = {
      println(s"unsubscribe for ")
    }
  }

  val server = new WAMPServer("localhost", 9090, "ws-greeter", Some(route), Nil).register( publisher, testWrites )
  val bindingFuture = server.bind

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  Console.readLine()

  bindingFuture.map(_())
}


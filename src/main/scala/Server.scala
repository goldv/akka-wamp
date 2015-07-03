import akka.http.scaladsl.server.Directives
import org.goldv.wampserver.server._
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

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
    def onSubscribe(sub: WAMPSubscriber[Test]) = {
      println(s"subscription received ${sub.topic}")
      val subscription = Await.result(sub.subscribed, 1 second)

      (1 to 10).foreach( i => subscription.publish( Test("blah", i) ))

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


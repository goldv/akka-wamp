import akka.http.scaladsl.server.Directives
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.goldv.wampserver.ServerTest
import com.typesafe.config.ConfigFactory
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

  val publisher = new WAMPPublisher {
    def baseTopic = "com.myapp"
    def onSubscribe(sub: WAMPSubscriber) = {
      println(s"subscription received ${sub.topic}")
      val subscription = sub.subscribed

      val mapper = new ObjectMapper()

      (1 to 10).foreach{ i =>
        val json: JsonNode = mapper.valueToTree( Test("blah", i))

        val pe = new PublicationEvent("test", "key", json)

        subscription.publish( pe )
      }

    }
    def onUnsubscribe(topic: String) = {
      println(s"unsubscribe for ")
    }
  }

  val config = """
    |akka {
    |
    |}
  """.stripMargin

  val server = new WAMPServer("localhost", 9090,"ws-greeter", "index.html")
    .register( publisher )
    .withDirResource("js", "js")

  val bindingFuture = server.bind

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  Console.readLine()

  bindingFuture.map(_())
}


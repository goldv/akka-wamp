import akka.http.scaladsl.server.Directives
import org.goldv.wampserver.server.WAMPServer

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

  val server = new WAMPServer("localhost", 9090, "ws-greeter", Some(route))
  val bindingFuture = server.bind

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  Console.readLine()

  bindingFuture.map(_())
}


package arimitsu.sf.benchmark

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import spray.json._
import DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow

import scala.concurrent.Future

object Bootstrap {
  implicit val format = jsonFormat1(JsonResponse)
  private def route = {
    val jsonRoute = get(path("json")(complete(JsonResponse(payload))))
    val plainTextRoute = get(path("plaintext")(complete(payload)))
    val wsRoute = path("ws") {
      extractUpgradeToWebSocket { upgrade =>
        complete(upgrade.handleMessages {
          Flow[Message].mapAsync(16)(_ => payloadAsJsonTextMessageF)
        })
      }
    }
    jsonRoute ~ plainTextRoute ~ wsRoute
  }
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("benchmark")
    implicit val materializer = ActorMaterializer()
    val routeHandler = Route.asyncHandler(route)
    Http().bindAndHandleAsync(routeHandler, "0.0.0.0", 9000, parallelism = 16)
  }
  private def payload = UUID.randomUUID().toString
  private def payloadAsJsonTextMessage = TextMessage(JsonResponse(payload).toJson.compactPrint)
  private def payloadAsJsonTextMessageF = Future.successful(payloadAsJsonTextMessage)
  case class JsonResponse(message: String)
}

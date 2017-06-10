package arimitsu.sf.benchmark

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Flow, Tcp }
import akka.util.ByteString
import org.apache.commons.lang3.RandomStringUtils

object Bootstrap {
  implicit val format = jsonFormat1(JsonResponse)
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("benchmark-system")
    implicit val materializer = ActorMaterializer()
    val jsonRoute = get(path("json")(complete(JsonResponse(payload))))
    val plainTextRoute = get(path("plaintext")(complete(payload)))
    val route = jsonRoute ~ plainTextRoute
    val routeHandler = Route.asyncHandler(route)
    Http().bindAndHandleAsync(routeHandler, "0.0.0.0", 9000, parallelism = 16)
  }
  private def payload = RandomStringUtils.randomAlphanumeric(100)
  case class JsonResponse(message: String)
}

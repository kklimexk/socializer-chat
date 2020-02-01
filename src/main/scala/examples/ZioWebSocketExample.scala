package examples

import cats.effect._
import fs2._
import fs2.concurrent.Queue
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.websocket._
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame._
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

import scala.concurrent.duration._

object ZioWebSocketExample extends CatsApp {

  private val zioWebSocketApp: ZioWebSocketExampleApp = new ZioWebSocketExampleApp()
  private val routes = zioWebSocketApp.routes

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    BlazeServerBuilder[Task]
      .bindHttp(8080)
      .withHttpApp(routes.orNotFound)
      .serve
      .compile[Task, Task, ExitCode]
      .drain
      .fold(_ => 1, _ => 0)
}

class ZioWebSocketExampleApp
  extends Http4sDsl[Task] {
  def routes: HttpRoutes[Task] = HttpRoutes.of[Task] {
    case GET -> Root / "hello" =>
      Ok("Hello world.")

    case GET -> Root / "ws" =>
      val toClient: Stream[Task, WebSocketFrame] =
        Stream.awakeEvery[Task](1.seconds).map(d => Text(s"Ping! $d"))
      val fromClient: Pipe[Task, WebSocketFrame, Unit] = _.evalMap {
        case Text(t, _) => Task(println(t))
        case f => Task(println(s"Unknown type: $f"))
      }
      WebSocketBuilder[Task].build(toClient, fromClient)

    case GET -> Root / "wsecho" =>
      val echoReply: Pipe[Task, WebSocketFrame, WebSocketFrame] =
        _.collect {
          case Text(msg, _) => Text("You sent the server: " + msg)
          case _ => Text("Something new")
        }

      /* Note that this use of a queue is not typical of http4s applications.
       * This creates a single queue to connect the input and output activity
       * on the WebSocket together. The queue is therefore not accessible outside
       * of the scope of this single HTTP request to connect a WebSocket.
       *
       * While this meets the contract of the service to echo traffic back to
       * its source, many applications will want to create the queue object at
       * a higher level and pass it into the "routes" method or the containing
       * class constructor in order to share the queue (or some other concurrency
       * object) across multiple requests, or to scope it to the application itself
       * instead of to a request.
       */
      Queue
        .unbounded[Task, WebSocketFrame]
        .flatMap { q =>
          val d = q.dequeue.through(echoReply)
          val e = q.enqueue
          WebSocketBuilder[Task].build(d, e)
        }
  }
}

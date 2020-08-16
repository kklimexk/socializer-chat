package com.example.api

import akka.NotUsed
import akka.event.Logging._
import akka.http.interop.{HttpServer, ZIOSupport}
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.{Flow, Source}
import com.example.application.ApplicationService
import zio.ZLayer
import zio.config.ZConfig
import zio.interop.reactivestreams._

object Api {

  trait Service {
    def routes: Route
  }

  val live: ZLayer[ZConfig[HttpServer.Config], Nothing, Api] = ZLayer.fromFunction(_ =>
    new Service with ZIOSupport {

      def routes: Route = httpRoute ~ webSocketRoute

      val httpRoute: Route =
        pathSingleSlash {
          getFromFile("frontend/index.html")
        }

      val webSocketRoute: Route =
        path("greeter") {
          logRequestResult(("greeter", InfoLevel)) {
            val greeterWebSocketService: Flow[Message, TextMessage, NotUsed] =
              Flow[Message].flatMapConcat {
                case tm: TextMessage if tm.getStrictText == "cmd1" =>
                  Source.futureSource(
                    unsafeRunToFuture(
                      ApplicationService.helloStream.toPublisher
                        .map(p =>
                          Source
                            .fromPublisher(p)
                            .map(m => TextMessage(Source.single(s"Hello $m") ++ tm.textStream))
                        )
                    )
                  )
              }
            handleWebSocketMessages(greeterWebSocketService)
          }
        }
    }
  )
}

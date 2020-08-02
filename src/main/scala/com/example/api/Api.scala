package com.example.api

import akka.event.Logging._
import akka.http.interop.ZIOSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.example.application.ApplicationService
import com.example.config.ApiConfig
import zio.ZLayer
import zio.config.Config

object Api {

  trait Service {
    def routes: Route
  }

  val live: ZLayer[Config[ApiConfig], Nothing, Api] = ZLayer.fromFunction(_ =>
    new Service with ZIOSupport {

      def routes: Route = httpRoute ~ webSocketRoute

      val httpRoute: Route =
        pathSingleSlash {
          getFromFile("frontend/index.html")
        }

      val webSocketRoute: Route =
        path("greeter") {
          logRequestResult(("greeter", InfoLevel)) {
            get {
              handleWebSocketMessages(ApplicationService.greeterWebSocketService)
            }
          }
        }
    }
  )
}

package com.example.api

import akka.event.Logging._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.example.application.ApplicationService
import com.example.config.ApiConfig
import com.example.interop.akka._
import zio.ZLayer
import zio.config.Config

object Api {

  trait Service {
    def routes: Route
  }

  val live: ZLayer[Config[ApiConfig], Nothing, Api] = ZLayer.fromFunction(env =>
    new Service with ZioSupport {

      def routes: Route = webSocketRoute

      val webSocketRoute: Route =
        path("greeter") {
          logRequestResult("greeter", InfoLevel) {
            get {
              handleWebSocketMessages(ApplicationService.greeterWebSocketService)
            }
          }
        }
    }
  )
}

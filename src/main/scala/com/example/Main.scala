package com.example

import akka.actor.ActorSystem
import akka.http.interop.HttpServer
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.example.api.Api
import com.example.config.AppConfig
import com.typesafe.config.ConfigFactory
import zio.config.{ZConfig, config}
import zio.config.typesafe.TypesafeConfig
import zio.console._
import zio.logging._
import zio.logging.slf4j._
import zio.{App, ExitCode, Has, TaskLayer, ULayer, ZIO, ZLayer, ZManaged}

object Main extends App {

  def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    ZIO(ConfigFactory.load.resolve).flatMap { rawConfig =>
      val configLayer = TypesafeConfig.fromTypesafeConfig(rawConfig, AppConfig.descriptor)

      val apiConfigLayer = configLayer.map(c => Has(c.get.api))

      val api = apiConfigLayer >>> Api.live
      val liveEnv = actorSystemLayer ++ Console.live ++ api ++ apiConfigLayer

      program.provideLayer(liveEnv)
    }.exitCode

  val program: ZIO[Console with Api with Has[ActorSystem] with ZConfig[HttpServer.Config], Throwable, Unit] =
    for {
      cfg <- config[HttpServer.Config]
      implicit0(system: ActorSystem) <- ZIO.access[Has[ActorSystem]](_.get[ActorSystem])
      api <- ZIO.access[Api](_.get)
      _ <- bindAndHandle(api.routes, cfg.host, cfg.port).use { _ =>
        for {
          _ <- putStrLn(
            s"Server online at http://${cfg.host}:${cfg.port}/\nPress RETURN to stop..."
          )
          _ <- getStrLn
        } yield ()
      }
    } yield ()

  def bindAndHandle(routes: Route, host: String, port: Int)(
    implicit system: ActorSystem
  ): ZManaged[Any, Throwable, Http.ServerBinding] =
    ZManaged.make(ZIO.fromFuture(_ => Http().bindAndHandle(routes, host, port)))(b =>
      ZIO.fromFuture(_ => b.unbind()).orDie
    )

  val loggingLayer: ULayer[Logging] = Slf4jLogger.make { (context, message) =>
    val logFormat = "[correlation-id = %s] %s"
    val correlationId = LogAnnotation.CorrelationId.render(
      context.get(LogAnnotation.CorrelationId)
    )
    logFormat.format(correlationId, message)
  }

  val actorSystemLayer: TaskLayer[Has[ActorSystem]] = ZLayer.fromManaged(
    ZManaged.make(ZIO.effect(ActorSystem("socializer-chat-system")))(s => ZIO.fromFuture(_ => s.terminate()).either)
  )

}

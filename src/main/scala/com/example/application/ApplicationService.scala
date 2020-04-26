package com.example.application

import akka.NotUsed
import akka.http.scaladsl.model.ws.{ Message, TextMessage }
import akka.stream.scaladsl.{ Flow, Source }

object ApplicationService {

  val greeterWebSocketService: Flow[Message, TextMessage, NotUsed] =
    Flow[Message].collect {
      case tm: TextMessage => TextMessage(Source.single("Hello ") ++ tm.textStream)
      // ignore binary messages
      // TODO #20096 in case a Streamed message comes in, we should runWith(Sink.ignore) its data
    }
}

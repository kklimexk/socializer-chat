package com.example.application

import zio.stream.ZStream

object ApplicationService {

  def helloStream: ZStream[Any, Nothing, String] =
    ZStream("World!!!")
}

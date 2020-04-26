package com.example.interop

import zio.Has

package object slick {
  type DatabaseProvider = Has[DatabaseProvider.Service]
}

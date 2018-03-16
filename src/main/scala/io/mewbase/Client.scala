package io.mewbase

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethod, HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.ActorMaterializer


object Client extends App {

  implicit val system = ActorSystem("client")
  import system.dispatcher

  implicit val materializer = ActorMaterializer()

  val ping = s"http://localhost:8200/publish/bbc7"
  val subs1 = s"http://localhost:8200/subscribe/bbc1"
  val subs2 = s"http://localhost:8200/subscribe/bbc2"
  val unsubs1 = s"http://localhost:8200/unsubscribe/bbc1"

  val entity  = """{ "name" : "Fred" }"""

  val publishRequest = Http().singleRequest(HttpRequest(POST, ping, entity = entity)).map { response =>
    println(response.entity.toString)
  }

  val sub1F = Http().singleRequest(HttpRequest(POST,  subs1)).flatMap { response =>
    response.entity.dataBytes.runForeach { chunk =>
      println(chunk.utf8String)
    }
  }

  Thread.sleep(1000)
    val another = Http().singleRequest(HttpRequest(POST, ping, entity = entity)).map { response =>
      println(response.entity.toString)
    }

  Thread.sleep(2500)



  val sub2F = Http().singleRequest(HttpRequest(POST,  subs2)).flatMap { response =>
    response.entity.dataBytes.runForeach { chunk =>
      println(chunk.utf8String)
    }
  }


}
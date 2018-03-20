package io.mewbase

import java.util.concurrent.{CompletableFuture, Future}

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethod, HttpMethods, HttpRequest, Uri}
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.ActorMaterializer


object Client extends App with Port {

  implicit val system = ActorSystem("client")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  println(s"Client using $getHostName:$getPortNumber")

  val ping = s"http://$getHostName:$getPortNumber/publish/bbc7"
  val subs1 = s"http://$getHostName:$getPortNumber/subscribe/bbc1"
  val subs2 = s"http://$getHostName:$getPortNumber/subscribe/bbc2"

  val entity  = """{ "name" : "Fred" }"""

  // Tests Simple ping
  val publishRequest = Http().singleRequest(HttpRequest(POST, ping, entity = entity)).map { response =>
    println(response.entity.toString)
  }


  // Test subscribe to channel capturing Subs ID in future.
  var chunkOne : CompletableFuture[String] = new CompletableFuture()
  val sub1F = Http().singleRequest(HttpRequest(POST,  subs1)).flatMap { response =>
    response.entity.dataBytes.runForeach { chunk =>
      if (!chunkOne.isDone()) chunkOne.complete(chunk.utf8String)
      println(chunk.utf8String)
    }
  }

  // Test interleave calls to the server
  Thread.sleep(3000)
    val anotherPing  = Http().singleRequest(HttpRequest(POST, ping, entity = entity)).map { response =>
      println(response.entity.toString)
    }

  Thread.sleep(2500)

  // Test more than one subscription concurrently
  val sub2F = Http().singleRequest(HttpRequest(POST,  subs2)).flatMap { response =>
    response.entity.dataBytes.runForeach { chunk =>
      println(chunk.utf8String)
    }
  }

  Thread.sleep(6500)

  // Test unsubscribe from one of the concurrent streams
  val subsId = chunkOne.get().split(":")(2)
  val unsubs1 = s"http://$getHostName:$getPortNumber/unsubscribe/"+ subsId
  val unsub = Http().singleRequest(HttpRequest(POST,  unsubs1)).flatMap { response =>
    response.entity.dataBytes.runForeach { chunk =>
      println(chunk.utf8String)
    }
  }

}
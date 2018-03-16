package io.mewbase

import java.util.concurrent.atomic.AtomicLong

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}


object Server extends App with Port
{

  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()


  val publishers : mutable.Map[String, ActorPublisher[String]] = mutable.Map[String, ActorPublisher[String]]()

  class StringActorPublisher extends ActorPublisher[String] {

    val counter = new AtomicLong(0)

    def pullElement() : Try[String] = {
      Thread.sleep(1000)
      Try( counter.getAndIncrement().toString() )
    }

    def cleanupResources() = {
      println("Cleaning up resources");
    }

    def receive = {
      case ActorPublisherMessage.Request(n) =>
        while (isActive && totalDemand > 0) {
          pullElement() match {
            case Success(event) =>
              onNext(event)
                // end of stream marker onComplete())
            case Failure(ex) =>
              onError(ex)
          }
        }

      case ActorPublisherMessage.Cancel =>
        // TODO - put an end chunk on the stream.
        cleanupResources()

      case ActorPublisherMessage.SubscriptionTimeoutExceeded =>
        cleanupResources()
    }
  }

  def createSource(channelName : String) : Source[String,NotUsed]#Repr[HttpEntity.ChunkStreamPart]  = {
    val publisher  = ActorPublisher[String](system.actorOf(Props(classOf[StringActorPublisher])))
    Source.fromPublisher(publisher).map {
      str => ChunkStreamPart.apply(s"Channel : $channelName EventNumber :$str") }
  }

  val subs : mutable.Map[String, Source[HttpEntity.ChunkStreamPart, Any]] = mutable.Map[String,Source[HttpEntity.ChunkStreamPart, Any]]()

  val pingRoute =
    get {
      path ( "ping" ) {
        println("pinged")
        complete("pong")
      }
    }

  val publishRoute =
    post {
      path("publish" / Segment ) { channelName =>
        entity(as[String]) { json =>
          println(channelName + " published " + json)
          complete(HttpEntity("pong"))
        }
      }
    }

  val subscribeRoute =
    post {
      path ("subscribe" / Segment ) { channelName =>
        println("subscribe to " + channelName)
        val source = createSource(channelName)
        complete(HttpEntity.Chunked(ContentTypes.`application/json`, source))
      }
    }

  val unsubscribeRoute =
    post {
      path ("unsubscribe" / Segment ) { channelName =>
        println("unsubscribe to " + channelName)
        // subs.get(channelName).map { pub =>
        // pub !
        // }
        complete( "unsubscribe" )
      }
    }

  val allRoutes =  { pingRoute ~ publishRoute ~ subscribeRoute }

  val serverSource = Http().bindAndHandle(allRoutes, interface = "localhost", getPortNumber)

  println (s"Server Started on port $getPortNumber")

}


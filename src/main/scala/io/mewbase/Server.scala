package io.mewbase

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.stream._
import akka.stream.scaladsl.{Sink, Source, SourceQueueWithComplete}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.stream.actor.{ActorPublisher, ActorPublisherMessage}
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}


import scala.concurrent.ExecutionContext.Implicits.global


object Server extends App with Port
{

  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()

  // Store a killSwitch associated with each
  val killSwitches = mutable.Map[String, Promise[Unit]]()



  class StringSource(id : UUID, killSwitch : Future[Unit]) extends GraphStage[SourceShape[String]] {
    // Define the (sole) output port of this stage
    val out: Outlet[String] = Outlet("StringSource")

    // Define the shape of this stage, which is SourceShape with the port we defined above
    override val shape: SourceShape[String] = SourceShape(out)


    // This is where the actual (possibly stateful) logic will live
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {

        var counter = 0

        override def preStart(): Unit = {
          val callback = getAsyncCallback[Unit] { (_) =>
            completeStage()
            println("Cleaning up resources")
          }
          killSwitch.foreach(callback.invoke)
        }

        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            if (counter == 0) push(out, id.toString())
            else push(out, counter.toString())
            counter += 1
            Thread.sleep(1000)
          }
        })
      }
  }


  def createStringSource(channelName : String) : Source[String,NotUsed]  = {
    val id = UUID.randomUUID
    val killSwitch = Promise[Unit]()
    killSwitches put(id.toString, killSwitch)
    Source.fromGraph( new StringSource(id, killSwitch.future ))
  }


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
          complete(HttpEntity("Event number:"+ 0))
        }
      }
    }

  val subscribeRoute =
    post {
      path ("subscribe" / Segment ) { channelName =>
        println("subscribe to " + channelName)
        val source = createStringSource(channelName).map {
          str => ChunkStreamPart(s"Channel : $channelName EventNumber :$str")
        }
        complete(HttpEntity.Chunked(ContentTypes.`application/json`, source))
      }
    }

  val unsubscribeRoute =
    post {
      path ("unsubscribe" / Segment ) {  subscriptionID =>
        println("unsubscribing " + subscriptionID)
        killSwitches.get(subscriptionID).foreach {  kSwitch =>
          kSwitch.complete( Try { () => () }  )
          killSwitches.remove(subscriptionID)
        }
        complete( "Unsubscribe:"+subscriptionID )
      }
    }

  val allRoutes =  { pingRoute ~ publishRoute ~ subscribeRoute ~ unsubscribeRoute }

  val serverSource = Http().bindAndHandle(allRoutes, interface = "localhost", getPortNumber)

  println (s"Server Started on port $getPortNumber")

}


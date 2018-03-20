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

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


object Server extends App with Port
{

  implicit val system = ActorSystem("server")
  implicit val materializer = ActorMaterializer()


  //*** Actor Publisher has been deprecated in favour of GraphStage
  // see
  case class StringActorPublisher() extends ActorPublisher[String] {

    val counter = new AtomicLong(0)

    val myActorName = self.path.name


    def pullElement() : Try[String] = {
      if (counter.get == 0L) {
        counter.getAndIncrement
        Try(myActorName)
      } else {
        Thread.sleep(1000)
        Try(counter.getAndIncrement.toString)
      }
    }

    def cleanupResources() = {
      println("Cleaning up resources");
    }


    import scala.concurrent.ExecutionContext.Implicits.global


    def receive = {
      case ActorPublisherMessage.Request(n) => {
        println(s"request for $n elements")
          while (isActive && totalDemand > 0) {
            pullElement() match {
              case Success(event) =>
                onNext(event)
              case Failure(ex) =>
                onError(ex)
            }
          }
        }


      case ActorPublisherMessage.Cancel => {
        println("Received cancel message")
        cleanupResources()
        onCompleteThenStop()
      }

      case ActorPublisherMessage.SubscriptionTimeoutExceeded =>
        cleanupResources()
    }

  } /* end of deprecated ActorPublisher */



  class StringSource(id : UUID) extends GraphStage[SourceShape[String]] {
    // Define the (sole) output port of this stage
    val out: Outlet[String] = Outlet("StringSource")
    // Define the shape of this stage, which is SourceShape with the port we defined above
    override val shape: SourceShape[String] = SourceShape(out)

    var counter = 0

    // This is where the actual (possibly stateful) logic will live
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
      new GraphStageLogic(shape) {
        setHandler(out, new OutHandler {
          override def onPull(): Unit = {
            if (counter == 0) push(out, id.toString())
            else push(out, counter.toString())
            counter += 1
            Thread.sleep(1000);
          }
        })
      }
  }


  def createStringSource(channelName : String) : Source[String,NotUsed]  = {
    val id = UUID.randomUUID
    //  val publisher  = ActorPublisher[String](system.actorOf(Props(classOf[StringActorPublisher]), actorUUID.toString))
    //    Source.fromPublisher(publisher).map {
    //    str => ChunkStreamPart.apply(s"Channel : $channelName EventNumber :$str") }

   Source.fromGraph( new StringSource(id) )
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
        system.actorSelection("akka://server/user/"+subscriptionID) ! ActorPublisherMessage.Cancel
        complete( "Unsubscribe:"+subscriptionID )
      }
    }

  val allRoutes =  { pingRoute ~ publishRoute ~ subscribeRoute ~ unsubscribeRoute }

  val serverSource = Http().bindAndHandle(allRoutes, interface = "localhost", getPortNumber)

  println (s"Server Started on port $getPortNumber")

}


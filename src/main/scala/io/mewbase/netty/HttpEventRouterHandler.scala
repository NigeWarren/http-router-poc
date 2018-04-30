package io.mewbase.netty

import java.util.UUID
import java.util.concurrent.Executors

import io.netty.buffer.Unpooled
import io.netty.channel._
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.websocketx._
import io.netty.util.CharsetUtil
import org.slf4j.LoggerFactory

import scala.collection.mutable


case class HttpEventRouterHandler() extends SimpleChannelInboundHandler[AnyRef] {


    private val logger = LoggerFactory.getLogger(classOf[HttpEventRouterHandler])

    val generators : mutable.Map[Channel, EventGenerator] = mutable.Map[Channel, EventGenerator]()


    @Override
    @throws[Exception]
    override protected def channelRead0(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {

      msg match {
        case httpRq : FullHttpRequest => this.handleHttpRequest(ctx, httpRq)
        case wsFrame : WebSocketFrame => handleWebSocketFrame(ctx, wsFrame)
      }

    }


  @throws[Exception]
  def handleHttpRequest(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = {


    req.method match {

      case HttpMethod.GET => {
        req.uri match {
          case "/ping" => complete(ctx,"pong")

          case _ => {
            // this get could be an upgrade to web socket request.
            val upgradeHeader = req.headers.get("Upgrade")
            if (upgradeHeader != null && "websocket".equalsIgnoreCase(upgradeHeader)) {
              val url = "ws://" + req.headers.get("Host") + req.uri
              val wsFactory = new WebSocketServerHandshakerFactory(url, null, false)
              val handshaker = wsFactory.newHandshaker(req)
              if (handshaker == null) {
                logger.debug("Handshaker is null")
                WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel)
              }  else {
                logger.debug("Handshaker is upgrading socket")
                handshaker.handshake(ctx.channel, req)
                logger.debug("Upgrading Handlers to Web Sockets Frame Handlers")
              }
              generators.put(ctx.channel, EventGenerator(ctx.channel, handshaker))
            } else {
              // or its just a random Get request that we dont handle
              error(ctx, HttpResponseStatus.BAD_REQUEST)
            }
          }
        }
      }


      // If you're going to do normal HTTP POST authentication before upgrading the
      // WebSocket, the recommendation is to handle it  here
      case HttpMethod.POST => {
        req.uri match {
          case "/publish" => {
            logger.debug("Published " + req.content().toString(CharsetUtil.UTF_8));
            complete(ctx,"0")
          }

          case "/subscribe" => {
            logger.debug("Subscribed " + req.content().toString(CharsetUtil.UTF_8));
            complete(ctx,UUID.randomUUID().toString())
          }

          case _ => { error(ctx, HttpResponseStatus.BAD_REQUEST) }
        }
      }

      case _  => error(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED)
    }

  }


  def handleWebSocketFrame(ctx: ChannelHandlerContext, frame: WebSocketFrame): Unit = {
    logger.debug("Received incoming ws frame [{}]", frame.getClass.getName)

    frame match {
      case closeFrame: CloseWebSocketFrame => {
        logger.debug("Close frame received on WebSocket");
        generators.get(ctx.channel).map { generator =>
          generator.handshaker.close(ctx.channel, frame.retain().asInstanceOf[CloseWebSocketFrame])
        }
        generators.remove(ctx.channel)
      }
      case  _ => logger.error("Received non closing web socket frame from client")
    }
  }


  // Complete a successful response with a given body
  def complete(ctx: ChannelHandlerContext, msg: String) = {
    val status = HttpResponseStatus.OK
    val message = Unpooled.copiedBuffer(msg, CharsetUtil.UTF_8)
    val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, message)
    response.headers.set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
    // Close the connection as soon as the content is sent.
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
  }


  // Send an unsucessful response with a given status response
  def error(ctx: ChannelHandlerContext, status : HttpResponseStatus ) = {
      val message = Unpooled.copiedBuffer("Failure: " + status , CharsetUtil.UTF_8)
      val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, message)
      response.headers.set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8")
      // Close the connection as soon as the error message is sent.
      ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE)
  }


  case class EventGenerator(val channel : Channel, val handshaker: WebSocketServerHandshaker) {

    Executors.newSingleThreadExecutor().submit( new Runnable {

      override def run(): Unit = {
        var counter = 0
        logger.debug("Event Generator Starter")
        while (channel.isActive) {
          channel.writeAndFlush(new TextWebSocketFrame("Message :"+counter))
          counter += 1
          Thread.sleep(1000L)
        }
        logger.debug("Writing Web Socket End Frame")
        channel.writeAndFlush( new CloseWebSocketFrame())
        logger.debug("Closing web socket channel")
        channel.close()
      }
    })

  }
}

package io.mewbase.netty

import java.util.UUID
import java.util.concurrent.Executors

import io.netty.buffer.Unpooled
import io.netty.channel.{Channel, ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.websocketx._
import io.netty.util.CharsetUtil
import org.slf4j.LoggerFactory


case class HttpEventRouterHandler() extends SimpleChannelInboundHandler[AnyRef] {


    private val logger = LoggerFactory.getLogger(classOf[HttpEventRouterHandler])

    protected var handshaker: WebSocketServerHandshaker = null


    @Override
    @throws[Exception]
    override protected def channelRead0(ctx: ChannelHandlerContext, msg: AnyRef): Unit = {

      msg match {
        case httpRq : FullHttpRequest => this.handleHttpRequest(ctx, httpRq)
        case wsFrame : WebSocketFrame => handleWebSocketFrame(ctx, wsFrame)
      }

    }


  @throws[Exception]
  def handleHttpRequest(ctx: ChannelHandlerContext, req: FullHttpRequest): Unit = { // Handle a bad request.

    // If you're going to do normal HTTP POST authentication before upgrading the
    // WebSocket, the recommendation is to handle it right here
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
              }
              EventGenerator(ctx.channel)
            }

            error(ctx, HttpResponseStatus.BAD_REQUEST)
          }
        }

      }

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
      case closeFrame: CloseWebSocketFrame => handshaker.close(ctx.channel, frame.retain().asInstanceOf[CloseWebSocketFrame])
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


  case class EventGenerator(val channel : Channel ) {

    Executors.newSingleThreadExecutor().submit( new Runnable {

      override def run(): Unit = {
        var counter = 0
        logger.debug("In Event Generator")
        while (channel.isActive) {
          logger.debug("In Event Generator loop " + channel.toString)
          counter += 1
          channel.writeAndFlush(new TextWebSocketFrame("Message :"+counter))
          Thread.sleep(200L)
        }
        logger.debug("Writing Web Socket End Frame")
        channel.writeAndFlush( new CloseWebSocketFrame())
        logger.debug("Closing web socket channel")
        channel.close()
      }
    })

  }
}
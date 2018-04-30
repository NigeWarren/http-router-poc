package io.mewbase.netty


// This is ported form the Java version here
// https://github.com/jwboardman/khs-stockticker

// Under this license
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpRequestDecoder, HttpResponseEncoder}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import org.slf4j.LoggerFactory


object HttpRouter extends App {


 class LocalChannelInitializer extends ChannelInitializer[SocketChannel] {
   override def initChannel(ch: SocketChannel): Unit = {
     val p = ch.pipeline()
     p.addLast("decoder", new HttpRequestDecoder(4096, 8192, 8192, false))
     p.addLast("aggregator", new HttpObjectAggregator(65536))
     p.addLast("encoder", new HttpResponseEncoder())
     p.addLast("handler", new HttpEventRouterHandler())
   }
 }


  private val logger = LoggerFactory.getLogger(classOf[Nothing])

  private val PORT = System.getProperty("port", "8080").toInt

    val bossGroup : NioEventLoopGroup = new NioEventLoopGroup(1)
    val workerGroup : NioEventLoopGroup = new NioEventLoopGroup()

    try {
      val b = new ServerBootstrap()

      b.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new LocalChannelInitializer() )

      // Start the server.
      val f = b.bind(PORT).sync
      logger.info("Http Router started")
      // Wait until the server socket is closed.
      f.channel.closeFuture.sync
      logger.info("Http Router closed")
    } finally {
      logger.info("Http Router shutdown started")
      // Shut down all event loops to terminate all threads.
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
      logger.info("Http Router shutdown completed")
    }

}

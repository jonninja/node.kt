package node.examples

import node.express.Express
import node.express.servers.ExpressNetty
import io.netty.channel.ChannelHandlerAdapter
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpRequest
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.channel.ChannelOption
import io.netty.handler.codec.http.HttpObject
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.CharsetUtil
import io.netty.handler.codec.http.HttpHeaders
import node.express.html.*

fun Body.cetHeader() {
    header(clas="cet-page-header") {
        div(clas="cet-page-header-content") {
            div(clas="cet-page-header-logo") {
                a(href="/linq") {
                    +"The Linq"
                }
            }
        }
    }
}

fun BodyTag.cetLinks() {

}

fun main(args: Array<String>) {
    val express = ExpressNetty()
    express.get("/test", {
        res.html(html {
            head {
                base("https://www.caesars.com")
                css("//maxcdn.bootstrapcdn.com/font-awesome/4.1.0/css/font-awesome.min.css")
                css("/etc/designs/caesars/foundation.min.cb31886de5456178d84abd0f8c5b091e.css")
                css("/content/cet-themes/default/linq.e618539793dc6cd332b058c1ae3225c9.css")
                meta("name" to "og:type", "content" to "website")
            }
            body {
                cetHeader()
            }
        })
    })
    express.listen(3100)
}
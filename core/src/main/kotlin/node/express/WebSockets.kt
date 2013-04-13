package node.express

import java.nio.ByteBuffer
import org.jboss.netty.channel.Channel
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame

/**
 * Provide an implementation of this class to listen for messages from a client.
 */
open class WebSocketHandler(val sender: WebSocketChannel) {
  /**
   * Called when a text message is received for this WebSocket
   */
  open fun message(content: String) {
  }

  /**
   * Called when a binary message is received on this WebSocket
   */
  open fun binaryMessage(data: ChannelBuffer) {
  }

  /**
   * Called when this socket has been closed
   */
  open fun closed() {
  }

  /**
   * Called when a ping is received
   */
  open fun ping() {
  }

  open fun handle(channel: Channel, frame: WebSocketFrame) {
    when (frame) {
      is CloseWebSocketFrame -> {
        this.closed()
      }
      is PingWebSocketFrame -> {
        this.ping()
        channel.write(PongWebSocketFrame((frame as PingWebSocketFrame).getBinaryData()))
      }
      is TextWebSocketFrame -> {
        this.message((frame as TextWebSocketFrame).getText()!!)
      }
      is BinaryWebSocketFrame -> {

      }
      else -> {

      }
    }
  }
}

/**
 * Handles sending of data to a web socket
 */
class WebSocketChannel(val channel: Channel) {
  /**
   * Send a text message to this WebSocket
   */
  fun send(content: String) {
    channel.write(TextWebSocketFrame(content))
  }

  /**
   * Send binary data to this WebSocket
   */
  fun send(data: ByteArray) {
    var byteBuffer = ByteBuffer.allocate(data.size)
    byteBuffer.put(data)
    channel.write(BinaryWebSocketFrame(org.jboss.netty.buffer.ByteBufferBackedChannelBuffer(byteBuffer)))
  }
}
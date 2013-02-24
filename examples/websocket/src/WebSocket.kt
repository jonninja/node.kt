/**
 * Example of using Node.kt's WebSocket support.
 */

import node.express.Express
import node.express.WebSocketChannel
import node.express.WebSocketHandler

class EchoHandler(sender: WebSocketChannel): WebSocketHandler(sender) {
  override fun message(content: String) {
    sender.send(content.toUpperCase())
  }
}

fun main(args: Array<String>) {
  var app = Express()
  app.webSocket("/echo", { sender ->
    EchoHandler(sender)
  })
  app.use(app.static("pages/"))
  app.listen(3000);
}
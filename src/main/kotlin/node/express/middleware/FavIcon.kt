package node.express.middleware

import node.express.Request
import node.express.Response
import node.express.Handler
import java.io.File
import node.express.RouteHandler

/**
 * FavIcon middleware, serves a site's Fav Icon
 */
public fun favIcon(path: String, maxAge: Long = 86400000): RouteHandler.()->Unit {
  var icon: ByteArray = File(path).readBytes()
  return {
    if (req.path == "/favicon.ico") {
      res.contentType("image/x-icon")
      res.header("Content-Length", icon.size.toString())
      res.header("Cache-Control", "public, max-age=" + (maxAge / 1000))
      res.send(icon)
    } else {
      next()
    }
  }
}
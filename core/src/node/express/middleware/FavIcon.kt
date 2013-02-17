package node.express.middleware

import node.express.Request
import node.express.Response
import node.express.Handler
import java.io.File

/**
 * FavIcon middleware, serves a site's Fav Icon
 */
class FavIcon(path: String): Handler {
  var icon: ByteArray = File(path).readBytes()
  var maxAge = 86400000

  fun withMaxAge(m: Int): FavIcon {
    this.maxAge = m
    return this
  }

  override fun exec(req: Request, res: Response, next: () -> Unit) {
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


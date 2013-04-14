package node.express.middleware

import node.express.Handler
import node.express.Request
import node.express.Response
import java.io.File
import java.util.HashMap
import java.io.FileNotFoundException

/**
 * Automatically processes templates relative to the location of the request
 */

class Renderer(): Handler {
  private val files = HashMap<String, Boolean>() // a cache of paths to files to improve performance

  override fun exec(req: Request, res: Response, next: () -> Unit) {
    val requestPath = req.param("*") as? String ?: ""

    var path = requestPath
    if (!path.startsWith("/")) {
      path = "/" + path
    }
    if (path.endsWith("/")) {
      path = "${path}index"
    }

    if (!files.containsKey(requestPath)) {
      try {
        res.render(path)
      } catch(e: FileNotFoundException) {
        files.put(requestPath, false)
        next()
      } catch(e: IllegalArgumentException) {
        files.put(requestPath, false)
        next()
      }
    } else {
      next()
    }
  }
}

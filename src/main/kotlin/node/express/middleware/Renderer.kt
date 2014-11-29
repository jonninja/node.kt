package node.express.middleware

import node.express.Handler
import node.express.Request
import node.express.Response
import java.io.File
import java.util.HashMap
import java.io.FileNotFoundException
import node.express.RouteHandler

/**
 * Automatically processes templates relative to the location of the request
 */
public fun renderer(): RouteHandler.()->Unit {
  val files = HashMap<String, Boolean>()
  return {
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
package node.express.middleware

import node.express.Request
import node.express.Response
import java.io.File
import java.util.HashMap
import node.express.RouteHandler

/**
 * Middleware for serving a tree of static files
 */
public fun static(basePath: String): RouteHandler.()->Unit {
  val files = HashMap<String, File>() // a cache of paths to files to improve performance
  return {
    if (req.method != "get" && req.method != "head")
      next()
    else {
      var requestPath = req.param("*") as? String ?: ""

      var srcFile: File? = files[requestPath] ?: {
        var path = requestPath
        if (!path.startsWith("/")) {
          path = "/" + path
        }
        if (path.endsWith("/")) {
          path += "index.html"
        }
        var f = File(basePath + path)
        if (f.exists()) {
          files.put(requestPath, f)
          f
        } else {
          null
        }
      }()

      if (srcFile != null) {
        res.sendFile(srcFile)
      } else {
        next()
      }
    }
  }
}
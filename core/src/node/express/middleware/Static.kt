package node.express.middleware

import node.express.Request
import node.express.Response
import java.io.File
import java.util.HashMap

/**
 * Middleware for serving a tree of static files
 */

class Static(basePath: String) {
  private val files = HashMap<String, File>() // a cache of paths to files to improve performance

  val basePath = basePath;
  fun callback(): (Request, Response, () -> Unit) -> Unit {
    return {(req, res, next) ->
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
            path = path + "index.html"
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
          res.sendFile(srcFile!!)
        } else {
          next()
        }
      }
    }
  }
}

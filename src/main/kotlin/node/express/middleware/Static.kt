package node.express.middleware

import node.express.Request
import node.express.Response
import java.io.File
import java.util.HashMap
import node.express.RouteHandler
import node.mimeType
import node.util._withNotNull

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

/**
 * Middleware that servers static resources from the code pacakage
 */
public fun staticResources(classBasePath: String): RouteHandler.()->Unit {
    return {
        if (req.method != "get" && req.method != "head")
            next()
        else {
            var requestPath = req.param("*") as? String ?: ""
            if (requestPath.length() > 0 && requestPath.charAt(0) == '/') {
                requestPath = requestPath.substring(1)
            }

            var resource = Thread.currentThread().getContextClassLoader().getResource(classBasePath + requestPath)
            if (resource != null) {
                _withNotNull(requestPath.mimeType()) { res.contentType(it) }
                resource.openStream().use {
                    res.send(it)
                }
            } else {
                next()
            }
        }
    }
}
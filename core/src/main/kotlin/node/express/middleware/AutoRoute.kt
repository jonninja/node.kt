package node.express.middleware

import node.express.Handler
import node.express.Request
import node.express.Response

/**
 * Middleware to automatically route requests to packages that complete the request
 */

class AutoRoute(val basePackage: String = "pages"): Handler {

  override fun exec(req: Request, res: Response, next: () -> Unit) {
    val requestPath = req.param("*") as? String ?: ""
    try {
      // first check for a direct match of the path
      val pathComponents = requestPath.split("/")
      val pkg = "${basePackage}${requestPath.replaceAll("/",".")}"
      val className = "${pkg}.${pathComponents[pathComponents.lastIndex].capitalize()}Package"
      val cls = Class.forName("${className}")
      val method = cls.getMethod("handle", javaClass<Request>(), javaClass<Response>())
      method.invoke(null, req, res)
    } catch(e: Throwable) {
      next()
    }
  }
}

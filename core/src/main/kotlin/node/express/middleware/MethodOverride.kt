package node.express.middleware

import node.express.Handler
import node.express.Request
import node.express.Response

/**
 * Provides faux HTTP method support.
 * Allows clients that can't send more obscure HTTP
 * methods like DELETE or PUT to include a parameter that is added to the request indicating
 * which method to use in a standard GET or POST request
 */
class MethodOverride(val key: String = "_method"): Handler {
  override fun exec(req: Request, res: Response, next: () -> Unit) {
    var override = req.param(key)
    if (override != null) {
      req.attributes["originalMethod"] = req.method
      req.method = (override as String).toLowerCase()
    }
    next();
  }
}
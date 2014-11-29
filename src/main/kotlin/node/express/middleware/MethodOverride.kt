package node.express.middleware

import node.express.Handler
import node.express.Request
import node.express.Response
import node.express.RouteHandler

/**
 * Provides faux HTTP method support.
 * Allows clients that can't send more obscure HTTP
 * methods like DELETE or PUT to include a parameter that is added to the request indicating
 * which method to use in a standard GET or POST request
 */
fun methodOverride(key: String = "_method"): RouteHandler.() -> Unit {
  return {
    var override = req.param(key)
    if (override != null) {
      req.attributes["originalMethod"] = req.method
      req.method = (override as String).toLowerCase()
    }
    next();
  }
}
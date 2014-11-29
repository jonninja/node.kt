package node.express.middleware

import node.express.Request
import node.express.Response
import node.express.Handler
import node.express.RouteHandler

/**
 * Middleware that parses bodies of various types and assigned to the body attribute of the request
 */
public fun bodyParser(): RouteHandler.()->Unit {
  return chained(jsonBodyParser(), urlEncodedBodyParser())
}
package node.express.middleware

import node.express.Request
import node.express.Response
import node.express.Handler
import node.express.RouteHandler

/**
 * Adds the `X-Response-Time` header displaying the response
 * duration in milliseconds.
 */
public fun responseTime(headerName: String = "X-Response-Time", format: String = "%dms"): RouteHandler.()->Unit {
  return {
    val start = System.currentTimeMillis();
    res.on("header", {(data) ->
      val time = System.currentTimeMillis() - start;
      res.header(headerName, java.lang.String.format(format, time));
    });
    next();
  }
}
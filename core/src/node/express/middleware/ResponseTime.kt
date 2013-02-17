package node.express.middleware

import node.express.Request
import node.express.Response
import node.express.Handler

/**
 * Adds the `X-Response-Time` header displaying the response
 * duration in milliseconds.
 */
class ResponseTime(val headerName: String = "X-Response-Time", val format: String = "%dms") : Handler {
  override fun exec(req: Request, res: Response, next: () -> Unit) {
    val start = System.currentTimeMillis();
    res.on("header", {(data) ->
      val time = System.currentTimeMillis() - start;
      res.header(headerName, java.lang.String.format(format, time));
    });
    next();
  }
}
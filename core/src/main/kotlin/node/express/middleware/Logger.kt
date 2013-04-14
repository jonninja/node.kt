package node.express.middleware

import node.express.Response
import node.express.Request
import node.util.logInfo
import node.express.Handler
import node.util._logger

/**
 * Logger middleware
 */
class Logger(): Handler {
  override fun exec(req: Request, res: Response, next: () -> Unit) {
    res.on("end", {
      var time = System.currentTimeMillis() - req.startTime
      req._logger.info(req.method + " " + req.path + " " + res.status + " " + time + "ms")
    })
    next()
  }
}
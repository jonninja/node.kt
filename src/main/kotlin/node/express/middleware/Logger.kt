package node.express.middleware

import node.express.Response
import node.express.Request
import node.util.logInfo
import node.express.Handler
import node.util._logger
import node.express.RouteHandler

/**
 * Logger middleware. Logs the method and path, as well as the length of time required to complete the request.
 */
fun logger(): RouteHandler.()->Unit {
    return {
        res.on("end", {
            var time = System.currentTimeMillis() - req.startTime
            req._logger.info(req.method + " " + req.path + " " + res.status + " " + time + "ms")
        })
        next()
    }
}
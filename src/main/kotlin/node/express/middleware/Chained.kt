package node.express.middleware

import node.express.RouteHandler
import node.express.Response
import node.express.Request

/**
 * A request handler that can chain other handlers.
 */
fun chained(vararg handlers: RouteHandler.()->Unit): RouteHandler.()->Unit {
    return {
        fun handleRequest(stackIndex: Int = 0) {
            val handlerExtension: RouteHandler.() -> Unit = handlers[stackIndex]
            val routeHandler = RouteHandler(req, res, { nextReq, nextRes ->
                if (stackIndex + 1 >= handlers.size) {
                    next()
                } else {
                    handleRequest(stackIndex + 1)
                }
            })
            routeHandler.handlerExtension()
        }
        handleRequest(0)
    }
}
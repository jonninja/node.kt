package node.kt.examples.autoroute

import node.express.Express
import node.express.middleware.Static

/**

Demonstrates how to create an server where requests are handled directly by the
rendering engine of choice.

*/

fun main(args: Array<String>) {
  var app = Express()

  app["view engine"] = "ftl" // the rendering engine
  app["views"] = "pages"
  app.use(node.express.middleware.AutoRoute(app, "pages"))
  app.use(node.express.middleware.Renderer())
  app.use(app.static(app["views"] as String))

  app.listen(4000)
}
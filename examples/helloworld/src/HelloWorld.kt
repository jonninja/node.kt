package node.kt.examples.HelloWorld;

import node.express.Express

/**
 * A Hello World sample using Node.kt
 */
fun main(args: Array<String>) {
  var app = Express()
  app.get("/", { req, res, next ->
    res.send("Hello World")
  })
  app.listen(3000)
}

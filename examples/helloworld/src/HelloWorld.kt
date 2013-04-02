package node.kt.examples.HelloWorld;

import node.express.Express
import node.util.log
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger
import node.http.HttpClient
import java.io.IOException

data class MyObject(val str: String, val str2: String = "steve")

/**
 * A Hello World sample using Node.kt
 */
fun main(args: Array<String>) {
  var app = Express()

  app.get("/:id", { req, res, next ->
    res.send(req.param("id") as String)
  })

  app.get("/", { req, res, next ->
    val data = req.data(javaClass<MyObject>())
    res.send(data.str2)
  })

  app.listen(3000)
}
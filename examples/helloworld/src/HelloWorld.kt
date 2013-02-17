package node.kt.examples.HelloWorld;

import node.express.Express
import node.express.middleware.CookieParser
import node.express.middleware.Logger
import node.express.middleware.session.*
import java.util.Date
import node.util.logInfo
import node.express.Response
import node.http.HttpMethod
import node.http.something
import com.fasterxml.jackson.databind.ObjectMapper
import node.util.objectNodeOf
import node.util.asNative
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import jet.runtime.typeinfo.JetMethod

Retention(RetentionPolicy.RUNTIME)
annotation class Foo

class User {
    Foo val uid: String? = null
    val name: Int = 0
}

class UserController {
}

/**
 * A Hello World sample using Node.kt
 */
fun main(args: Array<String>) {
  val methods = javaClass<User>().getMethods()
  for (m in methods) {
    val ann = m.getAnnotation(javaClass<Foo>())
    println(m.getName() + " " + ann)
  }
  val c = javaClass<User>().getConstructors()
  for (cu in c) {
    println(cu.getParameterTypes()!!.makeString(","))
  }


  var app = Express()
  app.enable("jsonp callback")
  app.use(Logger())
  app.use(CookieParser())
  app.use(CookieStoreSession())

  app.errorHandler = { t, req, res ->
    t.printStackTrace()
    res.send("We don't like this, but we'll send a default")
  }

  app.listen(4000);
}

package node.express.middleware

import node.express.Request
import node.express.Response
import node.express.Handler
import node.express.Cookie
import node.express.RouteHandler

/**
 * Defines a cookie parser
 */
public fun cookieParser(): RouteHandler.()->Unit {
  return {
    var cookiesString = req.header("cookie")
    var cookieMap = hashMapOf<String, Cookie>()
    if (cookiesString != null) {
      var pairs = cookiesString!!.split("[;,]")
      pairs.forEach {
        val cookie = Cookie.parse(it)
        if (!cookieMap.containsKey(cookie.key)) {
          cookieMap.put(cookie.key, cookie)
        }
      }
    }
    req.attributes.set("cookies", cookieMap)
    next()
  }
}
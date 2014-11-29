package node.express.middleware

import node.express.Handler
import node.express.Request
import node.express.Response
import node.util.splitToMap
import node.crypto.decode
import node.express.RouteHandler

/**
 * Middleware to parse (and optionally validate) basic authentication credentials. Credentials are
 * saved to the request attributes as 'username' and 'password'.
 */
public fun basicAuth(realm: String, validator: (String?, String?)->Boolean): RouteHandler.()->Unit {
  return {
    var username: String? = null;
    var password: String? = null;
    try {
      var authHeaderValue = req.header("authorization")
      if (authHeaderValue != null) {
        var auth = authHeaderValue!!.splitToMap(" ", "type", "data")
        val authString = auth["data"]!!.decode("base64")

        auth = authString.splitToMap(":", "username", "password")
        username = auth["username"]
        password = auth["password"]
      }
    } catch (t: Throwable) {
    }

    if (validator(username, password)) {
      if (username != null) {
        req.attributes["user"] = username!!
      }
      next()
    } else {
      res.header("WWW-Authenticate", "Basic realm=\"" + realm + "\"")
      res send 401
    }
  }
}

/**
 * Parses token authorization and calls your validator to ensure that the requestor has permissions
 * to access the resource.
 */
public fun tokenAuth(realm: String, validator: (String?,Request)->Boolean): RouteHandler.()->Unit {
  return {
    var token: String? = null
    var authHeaderValue = req.header("authorization")
    if (authHeaderValue != null) {
      val auth = authHeaderValue!!.splitToMap("=", "type", "token")
      if (auth["type"] == "token") {
        token = auth["token"]!!.replaceAll("^\"|\"$", "")
      }
    }
    if (validator(token, req)) {
      next()
    } else {
      res.header("WWW-Authenticate", "Basic realm=\"$realm\"")
      res send 401
    }
  }
}
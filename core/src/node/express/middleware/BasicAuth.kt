package node.express.middleware

import node.express.Handler
import node.express.Request
import node.express.Response
import node.util.splitToMap
import node.crypto.decode

/**
 * Middleware to parse (and optionally validate) basic authentication credentials
 */
class BasicAuth(val realm: String): Handler {
  var validator: (String?, String?) -> Boolean = { username, password ->
    true
  }

  fun requires(username: String, password: String): BasicAuth {
    validate { u, p ->
      u == username && p == password
    }
    return this
  }

  fun validate(validator: (String?, String?) -> Boolean) {
    this.validator = validator
  }

  override fun exec(req: Request, res: Response, next: () -> Unit) {
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

class TokenAuth(val realm: String, val validator: (String?, Request) -> Boolean): Handler {
  override fun exec(req: Request, res: Response, next: () -> Unit) {
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
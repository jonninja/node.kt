package node.express.middleware.session

import node.express.Request
import node.express.Response
import node.express.Handler
import java.util.HashMap
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import node.util.asNative
import node.util.putNative
import node.util.logDebug
import node.util.log
import java.util.logging.Level
import node.express.Cookie

// Interface for a session
trait Session {
  fun get(key: String): Any?
  fun set(key: String, value: Any)

  fun save()
  fun clear()
}

/**
 * A session that is no session at all and just stored data in memory
 * for the duration of the request
 */
class MemorySession: Session {
  val store = HashMap<String, Any>()

  override fun get(key: String): Any? {
    return store[key]
  }
  override fun set(key: String, value: Any) {
    store[key] = value;
  }
  override fun save() {
    // do nothing
  }
  override fun clear() {
    store.clear()
  }
}


/**
 * Base class for Session support. Default implementation just stores session data
 * in memory, which is not scalable across multiple servers.
 */
open
class SessionSupport(): Handler {
  override fun exec(req: Request, res: Response, next: () -> Unit) {
    val session = newSession(req, res)
    req.attributes["session"] = session
    res.on("header", { data ->
      session.save()
    })
    next()
  }
  open fun newSession(req: Request, res: Response): Session {
    return MemorySession()
  }
}

/**
 * Value extensions to ease access to the session object
 */
val Request.session: Session?
  get() = this.attributes["session"] as? Session;

val Response.session: Session?
  get() = this.req.attributes["session"] as? Session;

/**
 * A session where the entire contents of the session is stored in a cookie. Because of this,
 * clients need to be sure not to fill the session full of too much data (most browsers support
 * up to about 4K)
 */
class CookieStoreSession(val sessionKey: String = "_node_kt_session"): SessionSupport() {
  val json = ObjectMapper()
  var maxAge: Long = 0

  fun withMaxAge(maxAge: Long): CookieStoreSession {
    this.maxAge = maxAge;
    return this;
  }

  override fun newSession(req: Request, res: Response): Session {
    return CookieSession(req, res, sessionKey);
  }

  inner class CookieSession(val req: Request, val res: Response, val cookieName: String): Session {
    var jsonNode: ObjectNode? = null
    var hasChanged: Boolean = false

    {
      var cookieContent = req.cookie(cookieName)
      if (cookieContent != null) {
        try {
          var cookieObject = json.readTree(cookieContent) as? ObjectNode
          if (cookieObject != null) jsonNode = cookieObject!!
        } catch (t: Throwable) {
          log(Level.FINE, "Error parsing cookie: " + cookieContent, t)
        }
      }
    }

    override fun get(key: String): Any? {
      if (jsonNode == null) return null
      val result = jsonNode!!.get(key);
      return result?.asNative();
    }

    override fun set(key: String, value: Any) {
      hasChanged = true
      if (jsonNode == null) {
        jsonNode = json.createObjectNode()
      }
      jsonNode!!.putNative(key, value);
    }

    override fun save() {
      if (hasChanged) {
        if (jsonNode != null) {
          res.cookie(Cookie(cookieName, jsonNode.toString()).maxAge(maxAge))
        } else {
          // TODO: Need to clear the cookie
        }
      }
    }

    override fun clear() {
      hasChanged = true;
      jsonNode = json.createObjectNode()
    }
  }

}


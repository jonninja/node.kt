package node.express.facebook

import node.express.Handler
import node.express.Request
import node.express.Response
import node.crypto.hmac
import com.fasterxml.jackson.databind.ObjectMapper
import node.crypto.decode
import java.io.IOException
import node.util.log
import node.util.toMap

/**
 * Middleware for authenticating requests that include a Facebook session key. Validates the key
 * using a shared secret, then assigns the user id to facebookUser in the request.
 */
class FacebookAuthenticator(val secret: String, val paramName: String = "fbSessionKey"): Handler {
  val json = ObjectMapper()

  override fun exec(req: Request, res: Response, next: () -> Unit) {
    var signature = req.param(paramName) as? String
    if (signature == null) {
      signature = req.cookie(paramName)
    }
    signature = signature?.replace("-", "+")?.replace("_", "/")?.trim()
    if (signature != null) {
      val bits = signature!!.split("\\.").toMap("token", "data");

      val expected = bits["data"]?.hmac("HmacSHA256", secret, "base64")?.replace("=", "")
      if (expected == bits["token"]) {
        try {
          val jsonTree = json.readTree(bits["data"]?.decode("base64"))
          val uid = jsonTree?.get("user_id")?.asText()
          if (uid != null) {
            req.attributes["fbSessionKey"] = uid!!
            req.attributes["fbUid"] = uid!!
          }
        } catch (e: IOException) {
          this.log(message = "Error parsing json key", t = e)
        }
      }
    }
    next()
  }
}

val Request.facebookUser: String?
  get() = attributes.get("fbSessionKey") as? String

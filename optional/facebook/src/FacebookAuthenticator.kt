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
import node.http.HttpClient
import node.Configuration

/**
 * Middleware for authenticating requests that include a Facebook session key. Validates the key
 * using a shared secret, then assigns the user id to facebookUser in the request.
 */
class FacebookAuthenticator(val appId: String, val secret: String,
                            val success: (res: Response, accessToken: String)->Unit,
                            val fail: (res: Response)->Unit): Handler {
  val json = ObjectMapper()

  override fun exec(req: Request, res: Response, next: () -> Unit) {
    val code = req.param("code") as String?
    if (code != null) {
      val response = HttpClient.get("https://graph.facebook.com/oauth/access_token").
        query("client_id", appId).
        query("client_secret", secret).
        query("redirect_uri", "http://localhost:4000/users/login/facebook").
        query("code", code).form()

      val accessToken = response.get("access_token")
      if (accessToken != null) {
        success(res, accessToken)
      } else {
        fail(res)
      }
    } else {
      fail(res)
    }
//
//    var signature = req.param(paramName) as? String
//    if (signature == null) {
//      signature = req.cookie(paramName)
//    }
//    signature = signature?.replace("-", "+")?.replace("_", "/")?.trim()
//    if (signature != null) {
//      val bits = signature!!.split("\\.").toMap("token", "data");
//
//      val expected = bits["data"]?.hmac("HmacSHA256", secret, "base64")?.replace("=", "")
//      if (expected == bits["token"]) {
//        try {
//          val jsonTree = json.readTree(bits["data"]?.decode("base64"))
//          val uid = jsonTree?.get("user_id")?.asText()
//          if (uid != null) {
//            req.attributes["fbSessionKey"] = uid!!
//            req.attributes["fbUid"] = uid!!
//          }
//        } catch (e: IOException) {
//          this.log(message = "Error parsing json key", t = e)
//        }
//      }
//    }
//    next()
  }

  class object {
    /**
     * Creates a FacebookAuthenticator using information from configuration files. The paths
     * facebook.app.id and facebook.app.secret are used
     */
    fun withConfig(success: (res: Response, accessToken: String)->Unit,
                   fail: (res: Response)->Unit): FacebookAuthenticator {
      return FacebookAuthenticator(Configuration.string("facebook.app.id"),
          Configuration.string("facebook.app.secret"), success, fail)
    }
  }
}

val Request.facebookUser: String?
  get() = attributes.get("fbSessionKey") as? String

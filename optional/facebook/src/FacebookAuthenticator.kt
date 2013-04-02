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
class FacebookAuthenticator(val appId: String, val secret: String, val redirectUrl: String,
                            val success: (res: Response, accessToken: String)->Unit,
                            val fail: (res: Response)->Unit): Handler {
  val json = ObjectMapper();

  {
    this.log("Facebook authenticator. appId: $appId, redirectUrl: $redirectUrl")
  }

  override fun exec(req: Request, res: Response, next: () -> Unit) {
    this.log("Facebook authenticator. appId: $appId, redirectUrl: $redirectUrl")

    val code = req.param("code") as String?
    if (code != null) {
      val response = HttpClient.get("https://graph.facebook.com/oauth/access_token").
        query("client_id", appId).
        query("client_secret", secret).
        query("redirect_uri", redirectUrl).
        query("code", code).withErrorHandler({ r ->
        if (r.contentType() == "text/javascript") {
          throw IOException(r.text())
        } else {
          HttpClient.defaultErrorHandler(r)
        }
      }).form()

      val accessToken = response.get("access_token")
      if (accessToken != null) {
        success(res, accessToken)
      } else {
        fail(res)
      }
    } else {
      fail(res)
    }
  }

  class object {
    /**
     * Creates a FacebookAuthenticator using information from configuration files. The paths
     * facebook.app.id and facebook.app.secret are used
     */
    fun withConfig(success: (res: Response, accessToken: String)->Unit,
                   fail: (res: Response)->Unit): FacebookAuthenticator {
      return FacebookAuthenticator(Configuration.string("facebook.app.id"),
          Configuration.string("facebook.app.secret"), Configuration.string("facebook.loginurl"), success, fail)
    }
  }
}

val Request.facebookUser: String?
  get() = attributes.get("fbSessionKey") as? String

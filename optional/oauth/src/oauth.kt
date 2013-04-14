package node.oauth

import java.util.HashMap
import node.express.Express
import node.util.encodeUriComponent
import node.NotFoundException
import node.util.RandomStringGenerator
import node.http.HttpClient
import node.express.Response
import node.express.Request
import node.Configuration
import java.io.IOException

val stateGenerator = RandomStringGenerator(15)

/**
 * An OAuth client. Create an instance, then call register for each provider. To authorize, direct the user to
 * /oauth/login/:provider and the rest is taken care of
 */
class OAuth(val express: Express,
            val localPath: String = "/oauth/login/:provider") {
  val providers = HashMap<String, OAuthProvider>();

  {
    express.get(localPath, { req, res, next ->
      val providerId = req.param("provider") as String
      val provider = providers.get(providerId)
      if (provider == null) {
        throw NotFoundException()
      }

      val scope = req.param("scope") as String?
      val redirect = "http://${req.header("Host")}/oauth/callback/$providerId"
      res.redirect("${provider.authUrl}?client_id=${provider.clientId}&redirect_uri=${redirect.encodeUriComponent()}${
        if (scope != null){"&scope=${scope.encodeUriComponent()}"}else{""}}&state=${provider.state}")
    })

    express.get("/oauth/callback/:provider", { req, res, next ->
      val state = req.param("state") as String
      val code = req.param("code") as String

      val provider = providers.getOrElse(req.param("provider") as String) { throw NotFoundException() }
      if (provider.state != state) throw IllegalAccessException()

      val redirect = "http://${req.header("Host")}/oauth/callback/${provider.name}"
//      res.redirect("${provider.tokenUrl}?client_id=${provider.clientId}&redirect_uri=${redirect.encodeUriComponent()}&code=${code}&client_secret=${provider.secret}")

      val request = HttpClient.post(provider.tokenUrl).
        form("client_id", provider.clientId).
        form("redirect_uri", redirect).
        form("client_secret", provider.secret).
        form("code", code).withErrorHandler({ r ->
          if (r.contentType() == "text/javascript") {
            throw IOException(r.text())
          } else {
            HttpClient.defaultErrorHandler(r)
          }
        })
      request.accepts("*/*")
      val response = request.form()

      val accessToken = response.get("access_token")
      if (accessToken != null) {
        provider.authSuccess(req, res, accessToken)
      } else {
        provider.authFail(req, res)
      }
    })
  }

  fun register(provider: OAuthProvider) {
    providers.put(provider.name, provider)
  }

  /**
   * Register a provider, loading data from configuration. Configuration should be in oauth.${providername}
   * with keys authUrl, tokenUrl, clientId, secret
   */
  fun register(name: String, authSuccess: (Request, Response, String)->Unit,
               authFail: (Request, Response)->Unit) {
    register(OAuthProvider(name, Configuration.string("oauth.$name.authUrl"), Configuration.string("oauth.$name.tokenUrl"),
        Configuration.string("oauth.$name.clientId"), Configuration.string("oauth.$name.secret"), authSuccess, authFail))
  }
}

class OAuthProvider(val name: String, val authUrl: String, val tokenUrl: String,
                    val clientId: String, val secret: String,
                    val authSuccess: (Request, Response, String)->Unit,
                    val authFail: (Request, Response)->Unit) {
  val state = stateGenerator.next()
}
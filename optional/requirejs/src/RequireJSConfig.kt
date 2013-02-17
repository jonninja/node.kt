package node.express.middleware.requirejs
/**
 * Server side support for require-js configuration. Designed to make it easy
 * to configure a client with libraries and configuration options.
 */
import node.express.Handler
import node.express.Request
import node.express.Response
import java.util.HashMap
import node.util.toJsonString

private val basePath = "https://requirejs.node.kt/"
class RequireJSMiddleware: Handler {
  val libraries = HashMap<String, HashMap<String, Any>>()

  override fun exec(req: Request, res: Response, next: () -> Unit) {
    if (req.method == "get") {
      val config = buildConfig()
      val result = "requirejs.config(" + config.toJsonString() + ")"
      res.contentType("text/javascript")
      res.send(result)
    } else {
      next()
    }
  }

  /**
   * Build the configuration object
   */
  fun buildConfig(): Any {
    val paths = HashMap<String, Any>()
    val shim = HashMap<String, Any>()
    val config = HashMap<String, Any>()

    for (entry in libraries) {
      val libInfo: Map<String, Any> = entry.value
      if (libInfo.get("paths") != null) {
        paths.put(entry.getKey(), libInfo.get("paths")!!)
      }
      if (libInfo.get("exports") != null) {

      }
      if (libInfo.get("deps") != null) {

      }
      if (libInfo.get("config") != null) {

      }
    }

    return hashMapOf(
        "paths" to paths,
        "shim" to shim,
        "config" to config
    )
  }

  /**
   * Add a library to the list of requirement. Will pull the library definition from
   * a CDN, and use that in building a require-js configuration.
   * @throws IOException if the library isn't found in the CDN
   */
  fun require(library: String, config: Map<String, Any>? = null): RequireJSMiddleware {
    var libraryInfo = libraries.get(library)
    if (libraryInfo == null) {
      libraryInfo = node.http.Request.get(basePath + library + ".json").json() as HashMap<String, Any>
      val exports = libraryInfo!!.get("exports") as String
      val dependencies = libraryInfo!!.get("deps") as List<String>

      for (dependency in dependencies) {
        require(dependency) // load and configure all dependent libraries
      }

      if (config != null) {
        config(library, config)
      }
    }
    return this
  }

  /**
   * Adds module configuration information to the RequireJS config, but does not attempt to download
   * a library. Useful for passing configuration info to local modules.
   */
  fun config(library: String, c: Map<String, Any>): RequireJSMiddleware {
    var libraryInfo = libraries.get(library)
    if (libraryInfo == null) {
      libraryInfo = HashMap<String, Any>()
      libraryInfo!!.put(library, libraryInfo!!)
    }
    var config: HashMap<String, Any>? = libraryInfo!!.get("config") as? HashMap<String, Any>
    if (config == null) {
      config = HashMap<String, Any>()
      libraryInfo!!.put("config", config!!)
    }
    config!!.putAll(c)
    return this
  }
}
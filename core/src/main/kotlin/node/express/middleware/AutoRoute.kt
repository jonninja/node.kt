package node.express.middleware

import node.express.Handler
import node.express.Request
import node.express.Response
import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import org.reflections.util.ClasspathHelper
import org.reflections.util.FilterBuilder
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.ResourcesScanner
import com.google.common.base.Predicate
import node.util.after
import node.express.Express
import java.util.regex.Pattern
import node.util.until

/**
 * Middleware to automatically route requests to packages that complete the request
 */

class AutoRoute(val express: Express, val basePackage: String = "pages"): Handler {
  val filter: Predicate<String> = FilterBuilder().include(FilterBuilder.prefix(basePackage))!!
  val reflections = Reflections(ConfigurationBuilder()
      .setScanners(SubTypesScanner(false), ResourcesScanner())!!
      .setUrls(ClasspathHelper.forPackage(basePackage))!!
      .filterInputsBy(filter as Predicate<String?>));

  {
    val allClasses = reflections.getSubTypesOf(javaClass<Any>())!!
    for (cls in allClasses) {
      if (cls.getCanonicalName()!!.endsWith("Package")) {
        var pkgName = cls.getCanonicalName()!!.after(basePackage)
        pkgName = pkgName.substring(0, pkgName.lastIndexOf("."))
        if (pkgName.size == 0) {
          pkgName = "."
        }

        val path = pkgName.replaceAll("\\.", "/")

        val methods = cls.getDeclaredMethods()
        for (m in methods) {
          val hPath = "$path"
          val processedPath = hPath.replaceAll("_", ":")

          val paramTypes = m.getParameterTypes()!!
          val cb: ((Request, Response, ()->Unit)->Unit)? = if (paramTypes.size > 0 && paramTypes[0] == javaClass<Request>()) {
            if (paramTypes.size > 1 && paramTypes[1] == javaClass<Response>()) {
              if (paramTypes.size > 2) {
                { req, res, next->
                  m.invoke(null, req, res, next)
                }
              } else {
                { req, res, next->
                  m.invoke(null, req, res)
                }
              }
            } else {
              { req, res, next->
                m.invoke(null, req)
              }
            }
          } else null

          if (cb != null) {
            if (m.getName() == "get") express.get(processedPath, cb)
            if (m.getName() == "post") express.post(processedPath, cb)
            if (m.getName() == "put") express.put(processedPath, cb)
            if (m.getName() == "delete") express.delete(processedPath, cb)
          }
        }
      }
    }

    // now look for views
    val viewExtension = express["view engine"]
    val resources = reflections.getResources(Pattern.compile(".*$viewExtension\$"))!!
    for (resource in resources) {
      val view = resource.substring(basePackage.size).until(".$viewExtension")
      val path = if (view.endsWith("index")) view.until("index") else view
      val escaped = path.replaceAll("_", ":")
      express.all(escaped, { req, res, next ->
        res.render(path, mapOf("request" to req, "response" to res))
      })
    }
  }

  override fun exec(req: Request, res: Response, next: () -> Unit) {
    next()
  }
}

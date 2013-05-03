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

public class Pages(rootPackage: String): Handler {
  val reflections = Reflections(ConfigurationBuilder()
      .setScanners(SubTypesScanner(false), ResourcesScanner())!!
      .setUrls(ClasspathHelper.forPackage(rootPackage))!!);

  {
    val allClasses = reflections.getSubTypesOf(javaClass<Any>())!!
    for (cls in allClasses) {
      println(cls.getCanonicalName())
    }
  }


  override fun exec(req: Request, res: Response, next: () -> Unit) {
    throw UnsupportedOperationException()
  }
}
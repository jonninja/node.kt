package node.express.engines

import node.express.Engine
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader
import org.apache.velocity.runtime.RuntimeConstants
import org.apache.velocity.VelocityContext
import java.io.StringWriter

/**
 * An express engine that renders using Velocity
 */
class VelocityEngine(): Engine {
  val ve = org.apache.velocity.app.VelocityEngine();

  {
    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
    ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, "/");
    ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
  }

  override fun render(path: String, data: Map<String, Any?>): String {
    val context = VelocityContext(data);

    val template = ve.getTemplate(path)!!;
    val sw = StringWriter();
    template.merge(context, sw);
    return sw.toString();
  }
}
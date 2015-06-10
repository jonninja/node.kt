package node.express.engines

import node.express.Engine
import freemarker.template.Configuration
import freemarker.template.DefaultObjectWrapper
import java.io.StringWriter
import java.io.File

/**
 * Rendering engine using the Freemarker template system
 */
class FreemarkerEngine(): Engine {
  val fm = Configuration();

  init {
    fm.setObjectWrapper(DefaultObjectWrapper())
    fm.setDirectoryForTemplateLoading(File("/"))
  }

  override fun render(path: String, data: Map<String, Any?>): String {
    val template = fm.getTemplate(path);

    val writer = StringWriter();
    template!!.process(data, writer);
    writer.flush();
    return writer.toString();
  }
}
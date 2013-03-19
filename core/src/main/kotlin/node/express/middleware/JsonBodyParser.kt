package node.express.middleware

import node.express.Response
import node.express.Request
import org.jboss.netty.util.CharsetUtil
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.dom.attribute
import com.fasterxml.jackson.databind.JsonNode
import node.express.Body
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ArrayNode

private val json = ObjectMapper();

/**
 * Parses the body of a request as a JSON object
 */
val JsonBodyParser = {(req: Request, res: Response, next: () -> Unit) ->
  if (req.body == null) {
    try {
      var content = req.request.getContent()!!;
      if (content.readable()) {
        val jsonString = content.toString(CharsetUtil.UTF_8);
        val node = json.readTree(jsonString)!!;
        req.attributes.put("body", JsonBody(node));
      }
    } catch(t: Throwable) {
    }
  }
  next();
}

private class JsonBody(n: JsonNode): Body {
  var node = n;

  override fun get(key: String): Any? {
    var value = (node as? ObjectNode)?.get(key);
    return when  {
      value == null -> null
      value!!.isIntegralNumber() -> value!!.asInt()
      value!!.isTextual() -> value!!.asText()
      value!!.isBoolean() -> value!!.asBoolean()
      else -> value
    }
  }
  override fun asInt(key: String): Int? {
    return (node as? ObjectNode)?.get(key)?.asInt();
  }
  override fun asString(key: String): String? {
    return (node as? ObjectNode)?.get(key)?.asText();
  }
  override fun asInt(index: Int): Int? {
    return (node as? ArrayNode)?.get(index)?.asInt();
  }
  override fun asString(index: Int): String? {
    return (node as? ArrayNode)?.get(index)?.asText();
  }
  override fun asNative(): Any {
    return node;
  }
}
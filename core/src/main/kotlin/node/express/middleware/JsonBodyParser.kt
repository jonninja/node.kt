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
import java.util.ArrayList
import java.util.HashSet

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

  val objectNode: ObjectNode
    get() { return node as ObjectNode }

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


  public override fun size(): Int {
    return objectNode.size()
  }
  public override fun isEmpty(): Boolean {
    return objectNode.size() == 0
  }
  public override fun containsKey(key: Any?): Boolean {
    return objectNode.findValue(key as String?) != null
  }
  public override fun containsValue(value: Any?): Boolean {
    throw UnsupportedOperationException()
  }
  public override fun get(key: Any?): Any? {
    return objectNode.get(key as String?)?.textValue()
  }
  public override fun keySet(): Set<String> {
    return objectNode.fieldNames()!!.mapTo(HashSet<String>(), {it})
  }
  public override fun values(): Collection<Any?> {
    return objectNode.fields()!!.mapTo(ArrayList<Any?>(), {it})
  }
  public override fun entrySet(): Set<Map.Entry<String, Any?>> {
    return objectNode.fields()!!.mapTo(HashSet<Map.Entry<String, Any?>>(), {
      object : Map.Entry<String, Any?> {
        public override fun getKey(): String {
          return it.getKey()
        }
        public override fun getValue(): Any {
          return it.getValue()
        }
        public override fun hashCode(): Int {
          return it.hashCode()
        }
        public override fun equals(other: Any?): Boolean {
          return it.equals(other)
        }
      }
    })
  }
}
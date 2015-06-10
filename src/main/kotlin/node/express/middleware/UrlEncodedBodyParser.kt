package node.express.middleware

import node.express.Response
import node.express.Request
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import node.express.Body
import io.netty.handler.codec.http.multipart.Attribute
import io.netty.handler.codec.http.multipart.InterfaceHttpData
import java.util.HashSet
import node.express.RouteHandler

/**
 * Body parser for URL encoded data
 */
public fun urlEncodedBodyParser(): RouteHandler.()->Unit {
  return {
    if (req.body == null) {
      try {
        val decoder = HttpPostRequestDecoder(DefaultHttpDataFactory(false), req.request)
        val data = decoder.getBodyHttpDatas()!!
        if (data.size() > 0) {
          req.attributes.put("body", UrlEncodedBody(decoder))
        }
      } catch (e: Throwable) {
        // ignored
      }
    }
    next()
  }
}

private class UrlEncodedBody(decoder: HttpPostRequestDecoder): Body {
  val decoder = decoder

  private fun getAttribute(key: String): Attribute? {
    return decoder.getBodyHttpData(key) as? Attribute
  }
  override fun get(key: String): Any? {
    return getAttribute(key)?.getString()
  }
  override fun asInt(key: String): Int? {
    return getAttribute(key)?.getString()?.toInt()
  }
  override fun asString(key: String): String? {
    return getAttribute(key)?.getString()
  }
  override fun asInt(index: Int): Int? {
    throw UnsupportedOperationException()
  }
  override fun asString(index: Int): String? {
    throw UnsupportedOperationException()
  }
  override fun asNative(): Any {
    return decoder
  }
  public override fun size(): Int {
    return decoder.getBodyHttpDatas()!!.size()
  }
  public override fun isEmpty(): Boolean {
    return size() == 0
  }
  public override fun containsKey(key: Any?): Boolean {
    return decoder.getBodyHttpDatas()!!.find { it.getName() == key } != null
  }
  public override fun containsValue(value: Any?): Boolean {
    throw UnsupportedOperationException()
  }
  public override fun get(key: Any?): Any? {
    return this.get(key as String)
  }
  public override fun keySet(): Set<String> {
    return HashSet(decoder.getBodyHttpDatas()!!.map { it.getName()!! })
  }
  public override fun values(): Collection<Any?> {
    return decoder.getBodyHttpDatas()!!.map { (it as Attribute).getValue() }
  }
  public override fun entrySet(): Set<Map.Entry<String, Any?>> {
    return HashSet(decoder.getBodyHttpDatas()!!.map { BodyEntry(it as Attribute) })
  }
  private data class BodyEntry(val att: Attribute): Map.Entry<String, Any?> {
    public override fun hashCode(): Int {
      return att.hashCode()
    }
    public override fun equals(other: Any?): Boolean {
      return att.equals(other)
    }
    public override fun getKey(): String {
      return att.getName()!!
    }
    public override fun getValue(): Any? {
      return att.getValue()
    }
  }
}
package node.express.middleware

import node.express.Response
import node.express.Request
import org.jboss.netty.handler.codec.http.multipart.DefaultHttpDataFactory
import org.jboss.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import node.express.Body
import org.jboss.netty.handler.codec.http.multipart.Attribute
import org.jboss.netty.handler.codec.http.multipart.InterfaceHttpData

/**
 * Body parser for URL encoded data
 */
val UrlEncodedBodyParser = {(req: Request, res: Response, next: () -> Unit) ->
  if (req.body == null) {
    try {
      val decoder = HttpPostRequestDecoder(DefaultHttpDataFactory(false), req.request);
      val data = decoder.getBodyHttpDatas()!!;
      if (data.size() > 0) {
        req.attributes.put("body", UrlEncodedBody(decoder));
      }
    } catch (e: Throwable) {
      // ignored
    }
  }
  next();
}

private class UrlEncodedBody(decoder: HttpPostRequestDecoder): Body {
  val decoder = decoder;

  private fun getAttribute(key: String): Attribute? {
    return decoder.getBodyHttpData(key) as? Attribute;
  }

  override fun get(key: String): Any? {
    return getAttribute(key)?.getString()
  }
  override fun asInt(key: String): Int? {
    return getAttribute(key)?.getString()?.toInt();
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
    return decoder;
  }
}
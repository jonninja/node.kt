package node.express

import java.util.HashMap
import org.jboss.netty.handler.codec.http.QueryStringDecoder
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.handler.codec.http.HttpHeaders
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.Comparator
import node.mimeType

/**
 * The Http server request object
 */
class Request(app: Express, e: MessageEvent) {
  private val message = e;
  val app = app;

  var params: Map<String, Any> = HashMap<String, Any>();
  var route: Route? = null;
  var request = e.getMessage() as HttpRequest;
  var startTime = System.currentTimeMillis();
  var qsd = QueryStringDecoder(request.getUri());
  val attributes: MutableMap<String, Any> = HashMap<String, Any>();

  var method: String = request.getMethod()!!.getName()!!.toLowerCase()

  val path: String
    get() = qsd.getPath()!!

  val query = QueryString()

  /**
   * All of the cookies in a map
   */
  val cookies: Map<String, Cookie>
    get() {
      return attributes.get("cookies") as? Map<String, Cookie> ?: hashMapOf<String, Cookie>()
    }

  inner class QueryString() {
    fun get(key: String): String? {
      var p = qsd.getParameters();
      if (p != null) {
        var v = p!![key];
        if (v != null && v!!.size() > 0) {
          return v?.get(0);
        }
      }
      return null;
    }
  }

  /**
   * Get a cookie value. Returns null if the cookie is not found
   */
  fun cookie(key: String): String? {
    return cookies.get(key)?.value;
  }

  /**
   * Check if this request matches the given route. If so, set the parameters and route of this
   * request
   */
  fun checkRoute(route: Route, response: Response): Boolean {
    var params = route.match(this, response);
    if (params != null) {
      this.params = params!!;
      this.route = route;
      return true;
    } else {
      return false;
    }
  }

  /**
   * Get the value of a header
   */
  fun header(key: String): String? {
    val headers = request.getHeaders(key);
    if (headers?.size() == 0) return null;
    val head = headers?.head;
    return head;
  }

  /**
   * Check if this request is of a certain content type
   */
  fun isType(contentType: String): Boolean {
    var t = header(HttpHeaders.Names.CONTENT_TYPE)
    return if (t != null) {
      t.equals(contentType)
    } else {
      false
    }
  }

  /**
   * Get the remote ip address of the request
   */
  fun ip(): String {
    return message.getRemoteAddress().toString();
  }

  class Accept(val mainType: String, val subType: String, val quality: Double) {
    /**
     * Match a given mime type
     */
    fun match(t: String): Boolean {
      var mime: String ? = t;
      if (t.indexOf("/") == -1) {
        mime = t.mimeType();
      }
      if (mime == null) return false;
      var parts = mime!!.split("/");
      return (parts[0] == mainType || mainType == "*") &&
      (parts[1] == subType || subType == "*");
    }
  }

  private fun parseAccept(): List<Accept> {
    var accept = header(HttpHeaders.Names.ACCEPT);
    if (accept == null) return arrayList();

    var acceptArray = accept!!.split(",");
    return acceptArray.map<String, Accept> { it ->
      var result: Accept = Accept("", "", 1.0);
      var parts = it.split('/');
      if (parts.size == 2) {
        var quality = parts[1].split(";");
        if (quality.size == 2) {
          var qVal = quality[1].split("=")[1];
          result = Accept(parts[0], quality[0], qVal.toDouble());
        } else {
          result = Accept(parts[0], quality[0], 1.0);
        }
      }
      result;
    }.sort(object : Comparator<Accept> {
      public override fun compare(o1: Request.Accept, o2: Request.Accept): Int {
        if (o1.quality - o2.quality > 0) return -1;
        else if (o1.quality == o2.quality) return 0;
        else return 1;
      }
      public override fun equals(obj: Any?): Boolean {
        throw UnsupportedOperationException()
      }
    });
  }

  fun accepts(contentType: String): Boolean {
    var accepts = parseAccept();
    for (a in accepts) {
      if (a.match(contentType)) {
        return true;
      }
    }
    return false;
  }

  fun accepts(vararg contentTypes: String): String? {
    var accepts = parseAccept();
    for (a in accepts) {
      for (t in contentTypes) {
        if (a.match(t)) {
          return t;
        }
      }
    }
    return null;
  }

  val body: Body?
    get() = attributes.get("body") as? Body

  fun body(key: String): Any? {
    return body?.get(key);
  }

  /**
   * Look up the value of a parameter, first by checking path parameters, then the contents of the body,
   * then query parameters
   */
  fun param(key: String): Any? {
    var p = params.get(key)
    if (p == null) {
      p = this.body(key)
      if (p == null) {
        p = query.get(key)
      }
    }
    return p
  }

  fun requireParams(vararg names: String) {
    for (name in names) {
      if (params.get(name) == null) {
        throw ExpressException(400, "Missing parameter: $name")
      }
    }
  }
}
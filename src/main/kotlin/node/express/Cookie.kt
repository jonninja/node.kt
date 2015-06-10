package node.express

import java.util.Date
import java.util.ArrayList
import node.util.encodeUriComponent
import node.util.decodeUriComponent
import node.http.asHttpFormatString

/**
 * Encapsulates the data of a cookie.
 */
class Cookie(val key: String, val value: String) {
  var path: String? = "/"
  var expires: Date? = null
  var domain: String? = null
  var httpOnly: Boolean = false

  /**
   * Set the path for this cookie. The default is the root path
   */
  fun path(str: String): Cookie {
    path = str
    return this
  }

  /**
   * Set if this cookie is HTTP only, meaning it will only be available to HTTP requests
   * (and not Javascripts)
   */
  fun httpOnly(x: Boolean): Cookie {
    httpOnly = x;
    return this;
  }

  /**
   * Set the max age of this cookie. If not set, the cookie will expire after the session is
   * closed (ie. the browser window is closed)
   */
  fun maxAge(age: Long): Cookie {
    expires = Date(Date().getTime() + age)
    return this
  }

  /**
   * Convert the cookie to a string representation
   */
  override fun toString(): String {
    var pairs = ArrayList<String>()
    pairs.add(key + "=" + value.encodeUriComponent())
    if (domain != null) pairs.add("Domain=" + domain)
    if (path != null) pairs.add("Path=" + path)
    if (expires != null) pairs.add("Expires=" + expires!!.asHttpFormatString())
    if (httpOnly) pairs.add("HttpOnly");

    return pairs.join("; ");
  }

  companion object {
    /**
     * Parse a cookie string into a Cookie
     */
    fun parse(str: String): Cookie {
      var eq_idx = str.indexOf('=')
      if (eq_idx == -1) {
        return Cookie("", str.trim().decodeUriComponent())
      }
      var key = str.substring(0, eq_idx).trim()
      var value = str.substring(++eq_idx).trim().decodeUriComponent()

      if (value.charAt(0) == '\"') {
        value = value.substring(1, value.length() - 1)
      }
      return Cookie(key, value)
    }
  }
}
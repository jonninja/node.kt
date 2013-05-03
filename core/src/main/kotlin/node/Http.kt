package node.http;

import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpGet
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase
import org.apache.http.HttpException
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.util.EntityUtils
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import org.apache.http.entity.StringEntity
import java.io.InputStream
import java.util.ArrayList
import org.apache.http.message.BasicNameValuePair
import java.io.IOException
import org.apache.http.client.methods.HttpPut
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpHead
import org.apache.http.client.methods.HttpOptions
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.http.client.utils.URLEncodedUtils
import java.util.HashMap
import java.nio.charset.Charset
import node.NotFoundException
import java.util.logging.Level
import node.util._if
import org.apache.http.HttpResponseInterceptor
import org.apache.http.protocol.HttpContext
import node.util.log
import node.util.json.json
import node.util.logDebug
import org.apache.http.impl.conn.PoolingClientConnectionManager

/**
 * A simplified API for making HTTP requests of all kinds. The goal of the library is to support 98% of use
 * cases while maintaining an easy to use API. Complicated request scenarious are not supported - for those,
 * use a full-featured library like HttpClient.
 */

private val client = run {
  DefaultHttpClient(PoolingClientConnectionManager())
}

private val json = ObjectMapper();

enum class HttpMethod {
  GET
  POST
  PUT
  DELETE
  OPTIONS
  HEAD
  TRACE
}

/**
 * An HTTP request.
 */
class Request(request: HttpRequestBase) {
  private val request = request
  private var response: HttpResponse? = null
  private var formParameters: MutableList<NameValuePair>? = null; // stores form parameters

  /**
   * Called when the response has a status code >= 400. Default implementation throws an exception
   * and consumed the entity
   */
  var errorHandler: (Request)->Unit = HttpClient.defaultErrorHandler

  // The URL associated with this request
  val url: String = request.getURI().toString();

  {
    client.addResponseInterceptor(object: HttpResponseInterceptor {

      public override fun process(response: HttpResponse?, context: HttpContext?) {
        val entity = response!!.getEntity()
        if (entity != null) {
          val ceheader = entity.getContentEncoding()
          if (ceheader != null) {
            val codecs = ceheader.getElements()!!
            for (codec in codecs) {
              if (codec.getName()!!.equalsIgnoreCase("gzip")) {
                response.setEntity(org.apache.http.client.entity.GzipDecompressingEntity(response.getEntity()))
                return
              }
            }
          }
        }
      }
    })
  }

  /**
   * Get the value of a header. Returns null if the key doesn't exist in the response.
   */
  fun header(key: String): String? {
    connect();
    var headers = response?.getHeaders(key);
    return if (headers != null && headers!!.size > 0) {
      headers?.get(0)?.getValue();
    } else {
      null;
    }
  }

  fun withErrorHandler(handler: (Request)->Unit): Request {
    this.errorHandler = handler
    return this
  }

  /**
   * Set a request header
   */
  fun header(key: String, value: String): Request {
    request.setHeader(key, value);
    return this;
  }

  /**
   * Get the status code from the response
   */
  fun status(): Int {
    connect();
    return response!!.getStatusLine()!!.getStatusCode();
  }

  /**
   * Get the entire status line of the response
   */
  fun statusLine(): String {
    connect();
    return response!!.getStatusLine()!!.toString()!!
  }

  /**
   * Set the content type for a request
   */
  fun contentType(contentType: String): Request {
    return header("Content-Type", contentType);
  }

  /**
   * Get the content type for a request
   */
  fun contentType(): String? {
    return _if(header("Content-Type")) {
      val components = it.split(";")
      if (components.size > 0) components[0] else null
    }
  }

  /**
   * Add an accept header to the request
   */
  fun accepts(contentType: String) {
    request.setHeader("Accept", contentType)
  }

  /**
 * Get the body of the response as a json string, setting the appropriate Accept header
 */
  fun jsonString(): String? {
    // if we haven't connected, and there's no 'Accept' header, set it to
    // json to get the server to send us json
    if (response == null && request.getFirstHeader("Accept") == null) {
      request.setHeader("Accept", "application/json")
    }
    connect();
    return text()
  }

  /**
   * Parse the body of the response as json, mapped to a generic (Map or Array) object.
   */
  fun json(): Any {
    try {
      return jsonString()!!.json()
    } catch (e: Throwable) {
      throw IOException(e)
    }
  }

  /**
   * Parse the body of the response as json, mapped to the given type
   */
  fun <T> json(dataClass: Class<T>): T {
    try {
      return jsonString()!!.json(dataClass)
    } catch (e: Throwable) {
      throw IOException(e)
    }
  }

  /**
   * Set the body of the request to json. Sets the content type appropriately. This will only work with
   * request types that allow a body.
   */
  fun json(body: JsonNode): Request {
    contentType("application/json")
    val entity = StringEntity(body.toString()!!)
    (request as HttpEntityEnclosingRequestBase).setEntity(entity)
    return this;
  }

  /**
   * Retrieve a response as text.
   */
  fun text(): String? {
    connect()
    return EntityUtils.toString(response!!.getEntity()!!)
  }

  /**
   * Retrieve the response of type application/x-www-form-urlencoded.
   */
  fun form(): Map<String, String> {
    if (request.getFirstHeader("Accept") == null) {
      accepts("application/x-www-form-urlencoded")
    }
    connect()
    val str = EntityUtils.toString(response!!.getEntity()!!)
    val values = URLEncodedUtils.parse(str, Charset.forName("UTF-8"))!!
    val result = HashMap<String, String>()
    for (value in values) {
      result.put(value.getName()!!, value.getValue()!!)
    }
    return result
  }

  /**
   * Get the response body as an input stream.
   */
  fun body(): InputStream {
    connect()
    return response!!.getEntity()!!.getContent()!!
  }

  /**
   * Add form parameters. Since this call returns the Request object itself, it's fairly easy to chain calls
   * Request.post("http://service.com/upload").form("name","Some Name").form("age", 38)
   *
   * @param key the key of the form parameter
   * @param value the value of the form parameter
   */
  fun form(key: String, value: Any): Request {
    formParameters = formParameters ?: ArrayList<NameValuePair>()
    formParameters!!.add(BasicNameValuePair(key, value.toString()))
    return this;
  }

  /**
   * Add a query parameter to the request. Returns the request object to support chaining.
   */
  fun query(key: String, value: String): Request {
    val builder = URIBuilder(request.getURI()!!)
    builder.setParameter(key, value.toString())
    request.setURI(builder.build());
    return this;
  }

  /**
   * Consume the body of the response, leaving the connection
   * ready for more activity
   */
  fun consume(): Request {
    EntityUtils.consume(response!!.getEntity())
    return this
  }

  fun connect(): Request {
    if (response == null) {
      if (formParameters != null) {
        var entity = (request as HttpEntityEnclosingRequestBase).getEntity();
        if (entity != null) {
          throw HttpException("Multiple entities are not allowed. Perhaps you have set a body and form parameters?");
        }
        (request as HttpEntityEnclosingRequestBase).setEntity(UrlEncodedFormEntity(formParameters!!));
      }
      this.logDebug("Connecting to ${this.url}")
      response = client.execute(request);
      checkForResponseError()
    }
    return this;
  }

  private fun checkForResponseError() {
    val statusCode = response!!.getStatusLine()!!.getStatusCode()

    this.log("Connection to ${this.url} resulted in ${response!!.getStatusLine()!!.toString()}", Level.FINE)
    if (statusCode >= 400) {
      this.errorHandler(this)
    }
  }

  //
  //    /**
  //     * Pipe the data from a request to an output stream
  //     */
  //    fun pipe(outputStream: OutputStream) {
  //        var input : InputStream?;
  //        try {
  //            connect();
  //            input = response!!.getEntity()!!.getContent();
  //            IOUtils.copy(input, outputStream);
  //        } finally {
  //            try {
  //                if (input != null) input!!.close();
  //            } catch (e:IOException) { /* ignored */ }
  //        }
  //    }
  //
  //    /**
  //     * Pipe the data from a request to a file
  //     */
  //    fun pipe(file: File) {
  //        var input: InputStream?;
  //        try {
  //            connect();
  //            input = response!!.getEntity()!!.getContent();
  //            FileUtils.copyInputStreamToFile(input, file);
  //        } finally {
  //            try {
  //                if (input != null) input!!.close();
  //            } catch (e: Throwable) { /* ignored */ }
  //        }
  //    }
}

object HttpClient {
  val defaultErrorHandler: (Request)->Unit = { request->
    request.consume()
    if (request.status() == 404) {
      throw NotFoundException(request.url)
    } else {
      throw IOException(request.statusLine())
    }
  }

  /**
   * Initiate a GET request
   */
  fun get(url: String): Request {
    return Request(HttpGet(url))
  }

  fun post(url: String): Request {
    return Request(HttpPost(url));
  }

  fun put(url: String): Request {
    return Request(HttpPut(url));
  }

  fun delete(url: String): Request {
    return Request(HttpDelete(url));
  }

  fun head(url: String): Request {
    return Request(HttpHead(url));
  }

  fun options(url: String): Request {
    return Request(HttpOptions(url));
  }
}

val httpFormat = "EEE, dd MMM yyyy HH:mm:ss zzz";

/**
 * Format a date as an Http standard string
 */
fun Date.asHttpFormatString(): String {
  var sd = SimpleDateFormat(httpFormat);
  sd.setTimeZone(TimeZone.getTimeZone("GMT"));
  return sd.format(this);
}

/**
 * Parse an HTTP date string
 */
fun String.asHttpDate(): Date {
  var sd = SimpleDateFormat(httpFormat);
  sd.setTimeZone(TimeZone.getTimeZone("GMT"));
  return sd.parse(this)!!;
}

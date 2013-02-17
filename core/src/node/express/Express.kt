package node.express

import org.jboss.netty.bootstrap.ServerBootstrap
import java.util.HashMap
import org.jboss.netty.channel.SimpleChannelUpstreamHandler
import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.channel.MessageEvent
import org.jboss.netty.handler.codec.http.HttpRequest
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.Channels
import org.jboss.netty.handler.codec.http.HttpRequestDecoder
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.HttpResponseEncoder
import org.jboss.netty.handler.stream.ChunkedWriteHandler
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import java.util.concurrent.Executors
import java.util.ArrayList
import java.net.InetSocketAddress
import java.util.Date
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.regex.Pattern
import java.util.regex.Matcher
import node.express.middleware.Logger
import node.express.middleware.BodyParser
import node.express.middleware.Static
import node.express.engines.FreemarkerEngine
import node.express.engines.VelocityEngine
import node.util.extension
import java.io.File
import node.util.log
import java.util.logging.Level
import com.fasterxml.jackson.databind.ObjectMapper
import node.http.HttpMethod
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Retention
import node.NotFoundException

private val json = ObjectMapper();

/**
 * Express.kt
 */
class Express() {
  private val bootstrap: ServerBootstrap = ServerBootstrap(NioServerSocketChannelFactory(
      Executors.newCachedThreadPool(),
      Executors.newCachedThreadPool()))

  private val settings = HashMap<String, Any>()
  private val locals = HashMap<String, Any>()
  private val handlerStack: ArrayList<Route> = ArrayList<Route>()
  private val engines = HashMap<String, Engine>()

  val params = HashMap<String, (Request, Response, Any) -> Any>()

  var errorHandler: ((Throwable, Request, Response) -> Unit) = { t, req, res ->
    log(Level.WARNING, "Error thrown handling request: " + req.path, t)
    when (t) {
      is ExpressException -> res.send(t.code, t.getMessage())
      is NotFoundException -> res.notFound()
      is IllegalArgumentException -> res.badRequest()
      is IllegalAccessException -> res.forbidden()
      is UnsupportedOperationException -> res.notImplemented()
      else -> res.internalServerError()
    }
  }

  {
    bootstrap.setPipelineFactory(PipelineFactory(this))
    settings.put("views", "views/")
    settings.put("jsonp callback name", "callback")

    locals.put("settings", settings)

    engines.put("vm", VelocityEngine())
    engines.put("ftl", FreemarkerEngine())
  }

  /**
   * Start the server listening on the given port
   */
  fun listen(port: Int? = null) {
    var aPort = port;
    if (aPort == null) {
      aPort = get("port") as Int
    }
    if (aPort == null) {
      throw IllegalStateException("Port must be passed to listen or port property must be set")
    }
    bootstrap.bind(InetSocketAddress(aPort!!));
    this.log("Express listening on port " + port)
  }

  /**
   * Assign a setting value. Settings are available in templates as 'settings'
   */
  fun set(name: String, value: Any) {
    settings.put(name, value);
  }

  /**
   * Get the value of a setting
   */
  fun get(name: String): Any? {
    return settings.get(name);
  }

  /**
   * Register a rendering engine with an extension. Once registered, setting the 'view engine' setting
   * to a registered extension will set the default view engine. Then, when calling 'render', if no
   * file extension is part of the name, the default extension will be appended and the appropriate
   * rendering engine will be used.
   * @param ext the file extension to register (ie. vm, flt, etc.)
   * @param engine a rendering engine that implements the Engine trait
   */
  fun engine(ext: String, engine: Engine) {
    engines.put(ext, engine);
  }

  /**
   * Set a feature setting to 'true'. Identical to [[Express.set(feature, true)]].
   */
  fun enable(feature: String) {
    settings.put(feature, true)
  }

  /**
   * Set a feature setting to 'false'. Identical to [[Express.set(feature, false)]].
   */
  fun disable(feature: String) {
    settings.put(feature, false)
  }

  /**
   * Check if a setting is enabled. If the setting doesn't exist, returns false.
   */
  fun enabled(feature: String): Boolean {
    return settings.get(feature) as? Boolean ?: false
  }

  /**
   * Map logic to route parameters. When a provided parameter is present,
   * calls the builder function to assign the value. So, for example, the :user
   * parameter can be mapped to a function that creates a user object.
   */
  fun param(name: String, builder: (Request, Response, Any) -> Any) {
    params.put(name, builder)
  }

  /**
   * Render a view, returning the resulting string.
   * @param name the name of the view. If the extension is left off, the value of 'view engine' will
   * be used as the extension.
   * @param data data that will be passed to the rendering engine to be used in the rendering of the
   * view. This data will be merged with 'locals', allowing you to set global data to be used in
   * all rendering operations.
   */
  fun render(name: String, data: Map<String, Any>? = null): String {
    var ext = name.extension();
    var viewFileName = name;
    if (ext == null) {
      ext = settings.get("view engine") as String;
      if (ext == null) {
        throw IllegalArgumentException("No default view set for view without extension")
      }
      viewFileName = name + "." + ext;
    }

    val renderer = engines.get(ext);
    if (renderer == null) {
      throw IllegalArgumentException("No renderer for ext: " + ext);
    }

    var mergedContext = HashMap<String, Any>();
    mergedContext.putAll(locals);
    if (data != null) {
      mergedContext.putAll(data);
    }

    val viewsPath = settings.get("views") as String;

    val viewPath = File(viewsPath + viewFileName).getAbsolutePath();

    return renderer.render(viewPath, mergedContext);
  }

  fun install(method: String, path: String, handler: Handler) {
    var route = Route(method, path, handler);
    handlerStack.add(route)
  }

  private fun install(method: String,
                      path: String,
                      vararg middleware: (Request, Response, () -> Unit) -> Unit) {

    for (m in middleware) {
      install(method, path, NextFunHandler(m))
    }
  }

  private fun installAll(path: String, handler: Handler) {
    install("get", path, handler);
    install("put", path, handler);
    install("post", path, handler);
    install("delete", path, handler);
    install("head", path, handler);
  }
  /**
   * Install a middleware callback to be used for all requests.
   */
  fun use(callback: (Request, Response, () -> Unit) -> Unit) {
    install("*", "*", NextFunHandler(callback))
  }

  /**
   * Install a middleware Handler object to be used for all requests.
   */
  fun use(middleware: Handler) {
    install("*", "*", middleware)
  }

  /**
   * Install middleware for a given path expression
   */
  fun use(path: String, callback: (Request, Response, () -> Unit) -> Unit) {
    install("*", path, NextFunHandler(callback))
  }

  /**
   * Install a handler for a path for all HTTP methods.
   */
  fun all(path: String, vararg middleware: (Request, Response, () -> Unit) -> Unit) {
    install("*", path, *middleware)
  }

  /**
   * Install a GET handler callback for a path
   */
  fun get(path: String, vararg middleware: (Request, Response, () -> Unit) -> Unit) {
    install("get", path, *middleware);
  }

  /**
   * Install a POST handler callback for a path
   */
  fun post(path: String, vararg middleware: (Request, Response, () -> Unit) -> Unit) {
    install("post", path, *middleware);
  }

  /**
   * Install a PUT handler callback for a path
   */
  fun put(path: String, vararg middleware: (Request, Response, () -> Unit) -> Unit) {
    install("put", path, *middleware);
  }

  /**
   * Install a DELETE handler callback for a path
   */
  fun delete(path: String, vararg middleware: (Request, Response, () -> Unit) -> Unit) {
    install("delete", path, *middleware);
  }

  /**
   * Install a HEAD handler callback for a path
   */
  fun head(path: String, vararg middleware: (Request, Response, () -> Unit) -> Unit) {
    install("head", path, *middleware);
  }

  fun handleRequest(req: Request, res: Response, stackIndex: Int = 0) {
    if (stackIndex >= handlerStack.size) {
      res.send(404) // send a 404
    } else {
      var route = handlerStack[stackIndex]
      if (req.checkRoute(route, res)) {
        route.handler.exec(req, res, {
          handleRequest(req, res, stackIndex + 1)
        })
      } else {
        handleRequest(req, res, stackIndex + 1)
      }
    }
  }

//  /**
//   * Our Netty callback
//   */
//  private inner class RequestHandler(): SimpleChannelUpstreamHandler() {
//    fun messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
//      val req = Request(this@Express, e);
//      val res = Response(req, e);
//      try {
//        handleRequest(req, res, 0);
//      } catch (t: Throwable) {
//        errorHandler(t, req, res)
//      }
//    }
//  }
//
//  private inner class PipelineFactory(): ChannelPipelineFactory {
//    public override fun getPipeline(): ChannelPipeline? {
//      var pipeline = Channels.pipeline()!!;
//      pipeline.addLast("decoder", HttpRequestDecoder());
//      pipeline.addLast("aggregator", HttpChunkAggregator(1048576));
//      pipeline.addLast("encoder", HttpResponseEncoder());
//      pipeline.addLast("handler", RequestHandler());
//      pipeline.addLast("chunkedWriter", ChunkedWriteHandler());
//      return pipeline;
//    }
//  }
//
  /**
   * Get Logger middleware.
   */
  fun logger(): Logger {
    return Logger();
  }

  /**
   * Get middleware that parses JSON and UrlEncoded bodies
   */
  fun bodyParser(): Handler {
    return BodyParser();
  }

  /**
   * Middleware for serving static files
   */
  fun static(basePath: String): (req: Request, res: Response, next: () -> Unit) -> Unit {
    return Static(basePath).callback();
  }
}

/**
 * A trait for classes to handle requests. An application can install handlers as either a
 * callback function or an object that implements the Handler trait.
 */
trait Handler {
  fun exec(req: Request, res: Response, next: () -> Unit);

  class object {
    /**
     * Create a handler with a function callback suitable for middleware
     */
    fun middleware(callback: (req: Request, res: Response, next: () -> Unit) -> Unit): Handler {
      return NextFunHandler(callback)
    }
  }
}

/**
 * Handler that calls through to a function handler
 */
class NextFunHandler(val callback: (req: Request, res: Response, next: () -> Unit) -> Unit): Handler {
  override fun exec(req: Request, res: Response, next: () -> Unit) {
    this.callback(req, res, next);
  }
}

/**
 * Describes an installed route.
 */
class Route(val method: String, val path: String, val handler: Handler) {
  private class Key(val name: String, val optional: Boolean)

  var pattern = Pattern.compile("");
  var keys = ArrayList<Key>();

  {
    buildPathRegEx(path, true);
  }

  // Takes a simplified path and converts it to a RegEx that we can
  // use to filter requests
  fun buildPathRegEx(path: String, strict: Boolean) {
    var keys = ArrayList<Key>();
    var p = if (strict) {
      path
    } else {
      "/?"
    };

    var sb = StringBuffer();
    var pattern = Pattern.compile("(/)?(\\.)?:(\\w+)(?:(\\(.*?\\)))?(\\?)?(\\*)?");
    var matcher = pattern.matcher(p);
    while (matcher.find()) {
      val slash = matcher.group(1) ?: "";
      val format = matcher.group(2);
      val key = matcher.group(3);
      val capture = matcher.group(4);
      val optional = matcher.group(5);
      val star = matcher.group(6);

      if (key != null)
        keys.add(Key(key, optional != null));

      var replacement = "";
      if (optional == null) replacement += slash;
      if (format != null) replacement += format;

      if (capture != null) {
        replacement += capture;
      } else if (format != null) {
        replacement += "([^/.]+?)";
      } else {
        replacement += "([^/]+?)";
      }
      //            replacement += ")";
      if (optional != null) replacement += optional;

      if (star != null) replacement += "(/*)?";

      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    var regex = sb.toString();
    regex = regex.replaceAll("([/.])", "\\/");
    regex = regex.replaceAll("\\*", "(.*)");
    regex = "^" + regex + "$";
    this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    this.keys = keys;
  }

  /**
   * Match a request against the path specification, returning
   * a map of parameters
   */
  fun match(req: Request, res: Response): Map<String, Any>? {
    if (req.method != method && method != "*")
      return null // no match based on the method

    var path = req.path
    var matcher = pattern.matcher(path);

    if (matcher.matches()) {
      var result = HashMap<String, Any>();
      var count = matcher.groupCount();
      for (i in 1..count) {
        val value = matcher.group(i);
        if (value != null) {
          if (keys.size() >= i) {
            val key = keys.get(i - 1).name

            // now check with the app for any parameter mapping functions
            val mapper = req.app.params[key];
            if (mapper != null) {
              result.put(key, mapper(req, res, value))
            } else {
              result.put(key, value);
            }
          } else {
            result.put("*", value);
          }
        }
      }
      return result;
    } else {
      return null;
    }
  }
}

class ExpressException(val code: Int, msg: String? = null, cause: Throwable? = null): Exception(msg, cause)

private class PipelineFactory(val express: Express): ChannelPipelineFactory {
  public override fun getPipeline(): ChannelPipeline? {
    var pipeline = Channels.pipeline()!!;
    pipeline.addLast("decoder", HttpRequestDecoder());
    pipeline.addLast("aggregator", HttpChunkAggregator(1048576));
    pipeline.addLast("encoder", HttpResponseEncoder());
    pipeline.addLast("handler", RequestHandler(express));
    pipeline.addLast("chunkedWriter", ChunkedWriteHandler());
    return pipeline;
  }
}

private class RequestHandler(val express: Express): SimpleChannelUpstreamHandler() {
  fun messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val req = Request(express, e);
    val res = Response(req, e);
    try {
      express.handleRequest(req, res, 0);
    } catch (t: Throwable) {
      express.errorHandler(t, req, res)
    }
  }
}



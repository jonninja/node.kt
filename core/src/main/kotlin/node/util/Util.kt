package node.util

import jet.modules.Module
import java.util.logging.Logger
import java.util.logging.Level
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.Date
import java.text.SimpleDateFormat
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.LogManager
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import java.util.ArrayList
import java.util.Comparator
import java.util.logging.Formatter
import java.io.PrintWriter
import java.io.StringWriter

private var inited = false
fun init() {
  if (!inited) {
    val log = LogManager.getLogManager()!!.getLogger("")!!
    for (handler in log.getHandlers()) {
      handler.setFormatter(LogFormat)
    }
    inited = true
  }
}

private fun getLogger(a: Any?): Logger {
  init()
  if (a == null) {
    return Logger.getAnonymousLogger()
  } else {
    return Logger.getLogger(a.javaClass.getName())
  }
}

private val LINE_SEPARATOR = System.getProperty("line.separator")

object LogFormat: Formatter() {

  public override fun format(record: LogRecord): String {
    val sb = StringBuilder()

    sb.append(Date(record.getMillis()))
        .append(" ")
        .append(record.getLevel()!!.getLocalizedName())
        .append(": ")
        .append(formatMessage(record))
        .append(LINE_SEPARATOR);

    if (record.getThrown() != null) {
      try {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        record.getThrown()!!.printStackTrace(pw)
        pw.close()
        sb.append(sw.toString())
      } catch  (ex: Throwable) {
        // ignore
      }
    }
    return sb.toString();
  }
}

/**
 * Attaches a log function to any object, and uses that object's class as the
 * logging context.
 */
fun Any.log(message: Any, level: Level = Level.INFO, t: Throwable? = null) {
  getLogger(this).log(level, message.toString(), t);
}

fun Any.logSevere(message: Any, t: Throwable? = null) {
  this.log(message, Level.SEVERE, t)
}

fun Any.logDebug(message: Any, t: Throwable? = null) {
  this.log(message, Level.FINE, t)
}

/**
 * A whole bunch of extension functions
 */
public inline fun log(message: Any) {
  getLogger(null).log(Level.INFO, message.toString());
  System.out.println(message)
}

public inline fun log(l: Level, msg: String, t: Throwable? = null) {
  getLogger(null).log(l, msg, t);
}

public inline fun logInfo(message: Any) {
  log(Level.INFO, message.toString())
}

public inline fun logDebug(message: Any) {
  log(Level.FINE, message.toString())
}

public inline fun logWarning(message: Any) {
  log(Level.WARNING, message.toString())
}

public inline fun logSevere(message: Any) {
  log(Level.SEVERE, message.toString())
}

/**
 * Returns the native object of this node when it is a native type (String, int, etc.)
 * If not one of those types, returns a JsonNode
 */
fun JsonNode.asNative(key: String? = null): Any? {
  val node = if (key == null) this else this.get(key)
  if (node == null) return null

  return when {
    node.isTextual() -> node.asText()
    node.isInt() -> node.asInt()
    node.isBoolean() -> node.asBoolean()
    node.isLong() -> node.asLong()
    else -> node
  }
}

fun JsonNode.putNative(key: String, value: Any) {
  if (this is ObjectNode) {
    when (value) {
      is String -> this.put(key, value as String)
      is Int -> this.put(key, value as Int)
      is Boolean -> this.put(key, value as Boolean)
      else -> {
      }
    }
  }
}

/**
 * Create a JSON node like you create a Map
 */
fun <V> ObjectMapper.objectNodeOf(vararg values: Pair<String,V>): ObjectNode {
  val node = this.createObjectNode()!!
  for (p in values) {
    when (p.second) {
      is String -> node.put(p.first, p.second as String)
      is Int -> node.put(p.first, p.second as Int)
      is Boolean -> node.put(p.first, p.second as Boolean)
      is JsonNode -> node.put(p.first, p.second as JsonNode)
      else -> null
    }
  }
  return node
}

/**
 * Create a JSON array node with a list of JSON nodes
 */
fun ObjectMapper.arrayNodeOf(elements: List<JsonNode>): ArrayNode {
  val node = this.createArrayNode()!!
  node.addAll(elements)
  return node
}

fun <T,R> with(value: T?): (caller: (T?)->R?)->R? {
  return { cb ->
    cb(value)
  }
}

fun <T> List<T>.sort(comparator: (T, T)->Int) {
  this.sort(object: Comparator<T> {

    public override fun compare(o1: T, o2: T): Int {
      return comparator(o1, o2)
    }
    public override fun equals(obj: Any?): Boolean {
      return this.equals(obj)
    }
  })
}

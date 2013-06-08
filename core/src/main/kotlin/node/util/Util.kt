package node.util

import java.util.logging.Level
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.Date
import java.util.logging.LogRecord
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import java.util.Comparator
import java.util.logging.Formatter
import java.io.PrintWriter
import java.io.StringWriter
import java.util.HashMap

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

/**
 * Call a function with a given value. This is mostly a control flow construct that reduces the need
 * for temporary variables when you're testing the result of a function
 */
fun <T> _with(value: T, caller: (T)->Unit): T {
  caller(value)
  return value
}

fun <R:Any?> R?._else(cb: ()->R): R {
  if (this == null) {
    return cb()
  } else {
    return this
  }
}

/**
 * Does a falsey test, and calls the block only if it passes. The block is passed
 * the value of the test
 */
fun _if<T:Any,R:Any>(test: T?, cb: (T)->R?): R? {
  if (test == null || test == false || test == 0) {
    return null
  } else {
    return cb(test)
  }
}

fun _ifn<T:Any,R:Any>(test: T?, cb: ()->R?): R? {
  if (test == null || test == false || test == 0) {
    return cb()
  } else {
    return null
  }
}
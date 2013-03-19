package node.util

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

/**
 * Utilities for working with JSON
 */
private val json = ObjectMapper()

/**
 * Parse a JSON string into a JsonNode. Uses default parsing options
 */
fun String.json(): Any {
  return json.readValue(this, javaClass<Any>())!!
}

fun <T> String.json(dataClass: Class<T>): T {
  return json.readValue(this, dataClass)!!
}

fun Any.toJsonString(): String {
  return json.writeValueAsString(this)!!
}

/**
 * Read the data from a file as JSON
 */
fun File.json(): Any {
  return json.readValue(this, javaClass<Map<String, Any?>>())!!
}

/**
 * Read the data from a file as JSON
 */
fun <T> File.json(dataClass: Class<T>): T {
  return json.readValue(this, dataClass)!!
}
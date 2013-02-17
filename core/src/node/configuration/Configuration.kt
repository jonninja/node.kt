package node.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

/**
 * Easy to use API for loading and working with settings from files.
 */

private val json = ObjectMapper()
private val root = hashMapOf<String, Any?>()

fun loadSettings(vararg path: String) {
  for (p in path) {
    var data = json.readValue(File(p), javaClass<Map<String, Any?>>())!!;
    merge(root, data);
  }
}

private fun merge(m1: MutableMap<String, Any?>, m2: Map<String, Any?>) {
  var entries = m2.entrySet();
  for (entry in entries) {
    var srcValue = m1.get(entry.key)
    var value = entry.value
    if (srcValue == null) {
      m1.put(entry.key, value)
    } else {
      if (srcValue is Map<*, *> && value is Map<*, *>) {
        merge(srcValue as MutableMap<String, Any?>, value as Map<String, Any?>);
      } else {
        m1.put(entry.key, value);
      }
    }
  }
}

fun getSetting(path: String): Any? {
  var components = path.split("\\.");
  var value: Any? = root;
  for (component in components) {
    if (value != null && value is Map<*, *>) {
      value = (value as Map<*, *>).get(component);
    } else {
      return null;
    }
  }
  return value;
}

public object settings {
  fun get(path: String): Any? {
    return getSetting(path)
  }
}
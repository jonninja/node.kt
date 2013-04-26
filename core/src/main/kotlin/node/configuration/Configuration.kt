package node

import java.io.File
import java.util.HashMap
import java.io.FileNotFoundException
import node.util.log
import java.util.logging.Level.FINE
import node.util.json.*

/**
 * Easy to use API for loading and working with settings from files.
 */
public object Configuration {
  private var _root: HashMap<String, Any?>? = null
  val root: HashMap<String, Any?>  // should be private, but blocked by KT-3281
    get() {
      if (_root == null) {
        synchronized (this, {
          if (_root == null) {
            val configFile = System.getProperty("configuration.file") ?: "configuration.json"
            try {
              val newRoot = HashMap<String, Any?>()
              mergeFile(newRoot, configFile)
              _root = newRoot
            } catch (fnf: FileNotFoundException) {
              this.log("Configuration file could not be loaded", FINE)
            }
          }
        })
      }
      return _root!!
    }

  /**
   * Get the value of a setting
   * @param path the path to the setting
   * @returns the setting value. Null if not found.
   */
  fun get(path: String): Any? {
    var components = path.split("\\.");
    var value: Any? = this.root
    for (component in components) {
      if (value != null && value is Map<*, *>) {
        value = (value as Map<*, *>).get(component);
      } else {
        return null;
      }
    }
    return value;
  }

  /**
   * Get a value of a setting. If the value is not found, return the value returned from a provided
   * function. If the value is not found, the default funtion MUST be provided, otherwise exceptions will
   * be thrown.
   */
  fun get(path: String, def: (() -> Any)?): Any {
    return get(path) ?: def!!()
  }

  /**
   * Get a configuration value as a String. If the value is not present and a default callback is not provided,
   * exceptions will be thrown.
   */
  fun string(path: String, def: (() -> String)? = null): String {
    return get(path, def) as String
  }

  fun integer(path: String, def: (() -> String)? = null): Int {
    return get(path, def) as Int
  }

  fun map(path: String): Map<String, Any> {
    var m = get(path) as? Map<*,*>
    if (m == null) {
      m = HashMap<String, Any>()
    }
    return m as Map<String, Any>
  }

  /**
   * Load one or more configuration files that will be merged into this configuration
   */
  fun load(vararg path: String) {
    path.forEach { mergeFile(root, it) }
  }

  private fun mergeFile(target: MutableMap<String, Any?>, filePath: String): MutableMap<String, Any?> {
    this.log("Loading configuration file $filePath")
    var data = File(filePath).json(javaClass<Map<String, Any?>>())
    merge(target, data);
    return target
  }

  private fun merge(m1: MutableMap<String, Any?>, m2: Map<String, Any?>) {
    var entries = m2.entrySet();
    for (entry in entries) {
      if (entry.key == "include") {
        when (entry.value) {
          is String -> mergeFile(m1, (entry.value as String))
          is List<*> -> (entry.value!! as List<String>).forEach { mergeFile(m1, it) }
          else -> throw FormatException("Contents of include must be either String or list of Strings")
        }
      } else {
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
  }
}
package node.util

import java.lang.reflect.Method

/**
 * Some reflection APIs
 */
fun Any.method(name: String): ((vararg v: Any?) -> Any?)? {

  fun getCaller(m: Method): ((vararg v: Any?) -> Any?)? {
    return { args ->
      m.invoke(this, args);
    }
  }

  var methods = this.javaClass.getMethods();
  for (m in methods) {
    if (m.getName() == name) {
      return getCaller(m);
    }
  }
  return null;
}

fun Any.field(name: String): Any? {
  try {
    var f = this.javaClass.getField(name);
    return f.get(this);
  } catch (t: Throwable) {
    return null;
  }
}
package node.util

import java.util.HashMap

/**
 * Function that makes it easier to make lazily evaluated properties. The property
 * will be a function that returns the value.
 * Usage:
 *    val x: lazy { "some string" }
 *    ...
 *    x().toUpper()
 */
fun <T:Any> lazy(init: ()->T): ()->T {
  var _value: Any? = null
  var set = false
  return {
    if (!set) {
      _value = init()
      set = true
    }
    _value as T
  }
}

fun <T:Any> lazyNull(init: ()->T?): ()->T? {
  var _value: T? = null
  var set = false
  return {
    if (!set) {
      _value = init()
      set = true
    }
    _value
  }
}


private val lazyVals = HashMap<Any, ()->Any?>()

class Lazy<T>(val init: ()->T) {
  private var _value: Any? = null
  val value: T
    get() { return if (_value != null) value else { _value = init(); _value as T }}
}

fun <T:Any?> lazy(key: Any, init: ()->T): ()->T {
  return lazyVals.getOrPut(key, { lazy(init) }) as ()->T
}
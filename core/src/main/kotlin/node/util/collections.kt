package node.util

import java.util.Comparator
import java.util.HashMap
import java.util.LinkedHashMap

/**
 *
 */
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

fun <T,K> Iterable<T>.toMap(key: (T)->K): Map<K,T> {
  val result = LinkedHashMap<K, T>()
  this.forEach {
    result.put(key(it), it)
  }
  return result
}

fun <T,K> Iterable<T>.toMap(key: String): Map<K,T> {
  return this.toMap {
    it.property<K>(key).value
  }
}
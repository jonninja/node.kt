package node.util

import java.util.Comparator
import java.util.HashMap
import java.util.LinkedHashMap

fun <T,K> Iterable<T>.toMap(key: String): Map<K,T> {
  return this.toMap {
    it.property<K>(key).value
  }
}
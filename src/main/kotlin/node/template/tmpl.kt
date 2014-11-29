package node.template

import node.util._if

/**
 * A version of a while loop that takes the string output of each iteration
 * and concatenates it and returns it.
 */
fun While(eval: ()->Boolean, out: ()->String):String {
  val sb = StringBuilder()
  while (eval()) {
    _if(out()) { str->
      sb.append(str)
    }
  }
  return sb.toString()
}

/**
 * A version of a for loop that takes the string output of each iteration
 * and concatenates it and returns it
 */
fun For(iterator: Iterable<Any?>, out: (v:Any?)->String): String {
  return For(iterator.iterator(), out)
}

/**
 * A version of a for loop that takes the string output of each iteration
 * and concatenates it and returns it
 */
fun For(iterator: Iterator<Any?>, out: (v:Any?)->String?): String {
  val sb = StringBuilder()
  iterator.forEach {
    _if(out(it)) { txt->
      sb.append(txt)
    }
  }
  return sb.toString()
}
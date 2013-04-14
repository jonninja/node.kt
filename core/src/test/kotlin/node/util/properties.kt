package node.util.test

import org.junit.Test
import kotlin.test.assertEquals
import node.util.lazy

class PropertiesTest {
  Test fun lazyTest() {
    var callCount = 0

    val x = lazy {
      callCount ++
      "xxx"
    }
    assertEquals(callCount, 0)
    assertEquals("xxx", x())
    assertEquals(callCount, 1)
    assertEquals("xxx", x())
    assertEquals(callCount, 1)
  }

  var _xCallCount = 0
  val x: String
    get() { return lazy("x") { _xCallCount++; "xxx" }() }

  Test fun lazyGetterTest() {
    assertEquals(_xCallCount, 0)
    assertEquals("xxx", x)
    assertEquals(_xCallCount, 1)
    assertEquals("xxx", x)
    assertEquals(_xCallCount, 1)
  }

  var count = 0
  val something: String
    get() = lazy {
      count++
      "something"
    }()

  Test fun prop() {
    val x = something
    val y = something
    assertEquals(1, count)
  }
}
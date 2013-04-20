package node.inject

import org.junit.Test
import kotlin.test.assertEquals
import java.util.Date

trait TestTrait {
  val name: String

  class object {
    val clazz = javaClass<TestTrait>()
  }
}

class TestImpl: TestTrait {
  override val name: String = "impl"

  class object {
    val clazz = javaClass<TestImpl>()
  }
}

class Second(val test: TestTrait) {
  class object {
    val clazz = javaClass<Second>()
  }
}

class NamedTest(key("123") val foo: String) {

  class object {

  }
}

class SingletonTest {
 {
   SingletonTest.count++
 }

  class object {
    var count = 0
  }
}

class InjectTest {
  Test fun basics() {
    val bindings = Registry()
    bindings bind TestTrait.clazz toImplementation TestImpl.clazz

    val scope = DefaultScope(bindings)

    val impl = scope.instanceOf(TestTrait.clazz)
    assertEquals("impl", impl.name)
  }

  Test fun constructor() {
    val bindings = Registry()
    bindings bind Second.clazz
    bindings bind TestTrait.clazz toImplementation TestImpl.clazz

    val scope = DefaultScope(bindings)
    val second = scope.instanceOf(Second.clazz)
    assertEquals("impl", second.test.name)
  }

  Test fun named() {
    val bindings = Registry()
    bindings bind javaClass<NamedTest>()
    bindings bind "123" toInstance "bar"

    val scope = DefaultScope(bindings)
    val n = scope.instanceOf(javaClass<NamedTest>())
    assertEquals("bar", n.foo)
  }

  Test fun testSingleton() {
    val registry = Registry()
    registry bind javaClass<SingletonTest>() toSingleton(javaClass<SingletonTest>())

    val scope = DefaultScope(registry)
    scope.instanceOf(javaClass<SingletonTest>())
    assertEquals(1, SingletonTest.count)
    scope.instanceOf(javaClass<SingletonTest>())
    assertEquals(1, SingletonTest.count)

    var literalCount = 0
    registry bind javaClass<Date>() toSingletonFactory {scope->
      literalCount++
      Date()
    }

    val date = scope.instanceOf(javaClass<Date>())

  }
}
package node.inject

import org.junit.Test
import kotlin.test.assertEquals
import java.util.Date
import node.inject.CacheScope.*

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

class NamedTest(named("123") val foo: String) {

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
    val registry = Registry()
    registry bind TestTrait.clazz toImplementation TestImpl.clazz

    val factory = registry.factory()
    val impl = factory.instanceOf(TestTrait.clazz)
    assertEquals("impl", impl.name)
  }

  Test fun constructor() {
    val registry = Registry()
    registry bind Second.clazz
    registry bind TestTrait.clazz toImplementation TestImpl.clazz

    val factory = registry.factory()
    val second = factory.instanceOf(Second.clazz)
    assertEquals("impl", second.test.name)
  }

  Test fun named() {
    val registry = Registry()
    registry bind javaClass<NamedTest>()
    registry.bind(javaClass<String>(), "123") toInstance "bar"

    val factory = registry.factory()
    val n = factory.instanceOf(javaClass<NamedTest>())
    assertEquals("bar", n.foo)
  }

  Test fun testSingleton() {
    val registry = Registry()
    registry bind javaClass<SingletonTest>() withScope GLOBAL

    val factory = registry.factory()
    factory.instanceOf(javaClass<SingletonTest>())
    assertEquals(1, SingletonTest.count)
    factory.instanceOf(javaClass<SingletonTest>())
    assertEquals(1, SingletonTest.count)

    var literalCount = 0
    registry bind javaClass<Date>() toFactory {scope, name ->
      literalCount++
      Date()
    } withScope GLOBAL

    val date = factory.instanceOf(javaClass<Date>())
  }

  Test fun testScopes() {
    var count = 0
    val registry = Registry()

    registry bind javaClass<String>() toFactory { factory,name -> count++; "123" } withScope GLOBAL

    val factory = registry.factory()
    factory.instanceOf(javaClass<String>())
    assertEquals(1, count)
    factory.instanceOf(javaClass<String>())
    assertEquals(1, count)

    val factory2 = registry.factory()
    factory2.instanceOf(javaClass<String>())
    assertEquals(1, count)

    var opCount = 0
    registry bind javaClass<Int>() toFactory { factory,name -> opCount++; 1 } withScope OPERATION

    val opFactory = registry.factory(OPERATION)
    opFactory.instanceOf(javaClass<Int>())
    assertEquals(1, opCount)
    opFactory.instanceOf(javaClass<Int>())
    assertEquals(1, opCount)

    val opFactory2 = registry.factory(OPERATION)
    opFactory2.instanceOf(javaClass<Int>())
    assertEquals(2, opCount) // should have created a second instance
  }
}
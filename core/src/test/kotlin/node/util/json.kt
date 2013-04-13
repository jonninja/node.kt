package node.util

import org.junit.Test
import kotlin.test.assertEquals
import java.util.ArrayList
import node.http.HttpClient

class Kids : ArrayList<String>()

class Obj(val name: String, val kids: List<String>)
class Obj2(val name: String, val kids: Kids)

class Obj3(val name: String, val data: Any)

class JsonTest {
  Test fun testBasic() {
    assertEquals("test", "\"test\"".json(javaClass<String>()))
    assertEquals(1, "\"1\"".json(javaClass<Int>()))

    val str = "{\"name\":\"Jon\", \"kids\":[\"amy\",\"steve\",\"pete\"]}"
    val o = str.json(javaClass<Obj>())
    assertEquals("Jon", o.name)
    assertEquals(3, o.kids.size)
    assertEquals("amy", o.kids[0])

    val o2 = str.json(javaClass<Obj2>())
    assertEquals(3, o2.kids.size)
    assertEquals("amy", o2.kids[0])

    val o3Str = "{\"name\":\"Jon\", \"data\":[\"amy\",\"steve\",\"pete\"]}"
    val o3 = o3Str.json(javaClass<Obj3>())
    assertEquals("Jon", o3.name)

    val o4Str = "{\"name\":\"Jon\", \"data\":[\"amy\",\"steve\",{\"pete\":\"male\"}]}"
    val o4 = o4Str.json(javaClass<Any>()) as Map<*,*>
    assertEquals("Jon", o4.get("name"))
  }
}
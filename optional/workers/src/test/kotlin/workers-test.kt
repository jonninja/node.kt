package node.workers

import org.junit.Test
import kotlin.test.assertEquals

class SimpleWorker: Worker<String> {
  override fun process(data: String) {
    throw UnsupportedOperationException()
  }
}

class WorkersTest {
  fun <T> something() {
    println(javaClass<T>().getCanonicalName())
  }

  Test fun testPassThrough() {
    something<String>()

    val jobManager = JobManager(PassThroughQueue())
    val event = Event("test-event")

    var latest: String? = null
    jobManager.registerWorker(event, object: Worker<String> {
      override fun process(data: String) {
        latest = data
      }
    })
    jobManager.postEvent(event, "123").get()
    assertEquals("123", latest)
  }
}
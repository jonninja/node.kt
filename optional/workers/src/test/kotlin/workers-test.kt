package node.workers

import org.junit.Test
import kotlin.test.assertEquals
import node.inject.Registry

class SimpleWorker: Worker<String> {
  override fun process(data: String) {
    throw UnsupportedOperationException()
  }
}

class WorkersTest {
  Test fun testPassThrough() {
    val registry = Registry()
    val jobManager = JobManager(PassThroughQueue(), registry)
    val event = Event<String>("test-event")

    var latest: String? = null
    val worker = object: Worker<String> {
      override fun process(data: String) {
        latest = data
      }
    }
    registry.bind(javaClass<Worker<*>>(), "test-event") toInstance worker
    jobManager.postEvent(event, "123").get()
    assertEquals("123", latest)
  }
}
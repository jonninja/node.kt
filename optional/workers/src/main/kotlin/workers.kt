package node.workers

import java.util.concurrent.Future
import node.util.json.toJsonString
import java.util.concurrent.Executors
import node.util.json.json
import node.util._logger
import kotlin.concurrent.submit
import node.util.methods
import java.lang.reflect.Method
import node.inject.Registry
import node.inject.CacheScope

/**
 * A trait for instances that are capable of processing jobs
 */
trait Worker<EventDataType> {
  /**
   * Process data from a job.
   */
  fun process(data: EventDataType)
}

/**
 * Plugin for the JobManager that implements the actual queue. This could
 * be a simple in-memory queue or could use some more advanced fault tolerant
 * queue like AmazonAWS or RabbitMQ
 */
trait Queue {
  var jobHandler: (String)->Future<Boolean>

  /**
   * Post data to the queue. The return value lets us know when the queue is done
   * with the job. This may not mean that the job is actually completed, just that the
   * queue has done what it's going to do with it (which may or may not be successful)
   */
  fun post(data: String): Future<Boolean>

  /**
   * Start running the queue
   */
  fun run()
}

private class WorkerSpec(val worker: Worker<*>) {
  val method: Method;
  val dataType: Class<*>;
  {
    method = worker.javaClass.methods.find {
      it.getName() == "process" && it.getParameterTypes()!!.size == 1 &&
      it.getParameterTypes()!![0] != javaClass<Any>()
    }!!
    dataType = method.getParameterTypes()!![0]
  }
}

/**
 * Manages jobs and workers. Clients can post job events and data as well as
 * register event handlers.
 */
class JobManager(val queue: Queue, val registry: Registry) {
  val executor = Executors.newFixedThreadPool(10);

  {
    queue.jobHandler = {
      executor.submit {
        try {
          processData(it)
        } catch(t: Throwable) {
          this@JobManager._logger.warn("Error processing event: $it", t)
          false
        }
      }
    }
  }

  /**
   * Post an event to the work queue. Returns a future that can be
   * tested to check if the job was successfully posted. This doesn't
   * necessarily mean that the job has been completed... just that, at the
   * very least, the queue has posted the job.
   */
  fun <T> postEvent(evt: Event<T>, data: T): Future<Boolean> {
    return queue.post(hashMapOf(
        "evt" to evt.id,
        "data" to data.toJsonString(),
        "posted" to System.currentTimeMillis()
        ).toJsonString())
  }

  fun <T> processEvent(event: Event<T>, data: T): Boolean {
    val factory = registry.factory(CacheScope.OPERATION)
    val worker = factory.instanceOf(javaClass<Worker<*>>(), event.id)
    val spec = WorkerSpec(worker)
    spec.method.invoke(worker, data)
    return true
  }

  fun run() {
    queue.run()
  }

  private fun processData(data: String): Boolean {
    val eventData = data.json(javaClass<Map<String,*>>())
    val event = eventData.get("evt") as String

    val factory = registry.factory(CacheScope.OPERATION)
    val worker = factory.instanceOf(javaClass<Worker<*>>(), event)
    val spec = WorkerSpec(worker)

    val dataObj = (eventData.get("data") as? String?)?.json(spec.dataType as Class<Any>)
    spec.method.invoke(worker, dataObj)
    return true
  }
}

data class Event<T>(val id: String)

/**
 * Queue implementation that passes execution immediately to the job handler.
 */
class PassThroughQueue(): Queue {
  override var jobHandler: (String) -> Future<Boolean> = {
    throw IllegalStateException("Job handler not initialized")
  }

  override fun post(data: String): Future<Boolean> {
    return jobHandler(data)
  }

  override fun run() {
    throw UnsupportedOperationException()
  }
}
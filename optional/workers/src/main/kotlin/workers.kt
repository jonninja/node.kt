package node.workers

import java.util.HashMap
import java.util.concurrent.Future
import node.util.json.toJsonString
import java.util.concurrent.Executors
import node.util.json.json
import node.util._logger
import kotlin.concurrent.submit
import node.util.methods
import java.lang.reflect.Method
import java.util.concurrent.ExecutorService

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
class JobManager(val queue: Queue) {
  val workers = HashMap<Event, WorkerSpec>()
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
   * Register a new worker
   */
  fun <T> registerWorker(evt: Event, worker: Worker<T>) {
    if (workers.containsKey(evt)) {
      this._logger.warn("Attempting to re-register event.")
    }
    workers.put(evt, WorkerSpec(worker))
  }

  /**
   * Post an event to the work queue. Returns a future that can be
   * tested to check if the job was successfully posted. This doesn't
   * necessarily mean that the job has been completed... just that, at the
   * very least, the queue has posted the job.
   */
  fun <T> postEvent(evt: Event, data: T): Future<Boolean> {
    return queue.post(hashMapOf(
        "evt" to evt.id,
        "data" to data.toJsonString(),
        "posted" to System.currentTimeMillis()
        ).toJsonString())
  }

  fun <T> processEvent(event: Event, data: T): Boolean {
    val worker = workers.get(event)!!
    worker.method.invoke(worker.worker, data)
    return true
  }

  private fun processData(data: String): Boolean {
    val eventData = data.json(javaClass<Map<String,*>>())
    val event = Event(eventData.get("evt") as String)

    val worker = workers.get(event)
    val dataObj = (eventData.get("data") as? String?)?.json(worker!!.dataType as Class<Any>)
    worker.method.invoke(worker.worker, dataObj)
    return true
  }
}

data class Event(val id: String)
val Events = Event("events")

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
}
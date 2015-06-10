package node

import java.util.HashMap
import java.util.ArrayList

/**
 * Provides a core event emitter. Any class can subclass this to provide event capabilities
 */
open class EventEmitter() {
  private val listeners = HashMap<String, MutableList<(Any?) -> Unit>>();

  /**
   * Install an event listener
   * @param event the event name
   * @param listener a function to be called when the event is fired
   */
  fun on(event: String, listener: (Any?) -> Unit) {
    var l = listeners[event];
    if (l == null) {
      l = ArrayList<(Any?) -> Unit>();
      listeners.put(event, l);
    }
    l.add(listener)
  }

  /**
   * Emit an event
   * @param event the event name
   * @param data event data
   */
  fun emit(event: String, data: Any?) {
    var l = listeners[event];
    if (l != null) {
      for (listener in l.iterator()) {
        listener(data);
      }
    }
  }
}
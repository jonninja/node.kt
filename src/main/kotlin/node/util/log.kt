package node.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.logging.Level

private fun getLogger(a: Any?): Logger {
  if (a == null) {
    return LoggerFactory.getLogger("ROOT")!!
  } else {
    return LoggerFactory.getLogger(a.javaClass)!!
  }
}

public inline fun <reified T:Any> loggerFor(): Logger = LoggerFactory.getLogger(javaClass<T>())

/**
 * Attaches a log function to any object, and uses that object's class as the
 * logging context.
 */
fun Any.log(message: Any, level: Level = Level.INFO, t: Throwable? = null) {
  val logger = getLogger(this)
  val msg = message.toString()
  when (level) {
    Level.INFO -> logger.info(msg, t);
    Level.SEVERE -> logger.error(msg, t)
    Level.WARNING -> logger.warn(msg, t)
    Level.FINE -> logger.debug(msg, t)
    Level.FINER -> logger.debug(msg, t)
    Level.FINEST -> logger.debug(msg, t)
    else -> logger.info(msg, t)
  }
}

val Any?._logger: Logger
  get() {
    return getLogger(this)
  }

fun Any.logSevere(message: Any, t: Throwable? = null) {
  getLogger(this).error(message.toString(), t)
}

fun Any.logDebug(message: Any, t: Throwable? = null) {
  getLogger(this).debug(message.toString(), t)
}

public fun log(message: Any) {
  getLogger(null).info(message.toString())
}

public fun log(l: Level, msg: String, t: Throwable? = null) {
  val logger = getLogger(null)
  when (l) {
    Level.INFO -> logger.info(msg, t);
    Level.SEVERE -> logger.error(msg, t)
    Level.WARNING -> logger.warn(msg, t)
    Level.FINE -> logger.debug(msg, t)
    Level.FINER -> logger.debug(msg, t)
    Level.FINEST -> logger.debug(msg, t)
    else -> logger.info(msg, t)
  }
}

public fun logInfo(message: Any) {
  log(Level.INFO, message.toString())
}

public fun logDebug(message: Any) {
  log(Level.FINE, message.toString())
}

public fun logWarning(message: Any) {
  log(Level.WARNING, message.toString())
}

public fun logSevere(message: Any) {
  log(Level.SEVERE, message.toString())
}

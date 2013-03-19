package node.util.date

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar

/**
 * Some utilities and extension for working with dates
 */

fun Date.format(pattern: String): String {
  return SimpleDateFormat(pattern).format(this)
}

fun String.toDate(pattern: String): Date {
  return SimpleDateFormat(pattern).parse(this)!!
}

fun Date.toISO8601String(): String {
  val cal = Calendar.getInstance()
  cal.setTime(this)
  return javax.xml.bind.DatatypeConverter.printDateTime(cal)!!
}

fun String.parseISO8601(): Date {
  val cal = javax.xml.bind.DatatypeConverter.parseDateTime(this)!!
  return cal.getTime()
}

fun Int.seconds(): Long = this.toLong() * 1000
fun Int.minutes(): Long = (this*60).seconds()
fun Int.hours(): Long = (this*60).minutes()
fun Int.days(): Long = (this*24).hours()
fun Int.weeks(): Long = (this*7).days()
fun Int.years(): Long = (this*365).days()

fun Long.inSeconds(): Long = (this/1000) // converts the time in ms to seconds


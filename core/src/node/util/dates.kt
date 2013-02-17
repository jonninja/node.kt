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

fun Date.toISO8601(): String {
  val cal = Calendar.getInstance()
  cal.setTime(this)
  return javax.xml.bind.DatatypeConverter.printDateTime(cal)!!
}

fun Int.seconds(): Long = this.toLong() * 1000
fun Int.minutes(): Long = (this*60).seconds()
fun Int.hours(): Long = (this*60).minutes()
fun Int.days(): Long = (this*24).hours()
fun Int.weeks(): Long = (this*7).days()


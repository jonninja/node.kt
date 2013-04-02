// An sample project that includes usage of a bunch of random bits of the framework
package node.kt.examples.Misc;

import node.Configuration
fun main(args: Array<String>) {

  val value = Configuration.get("value")
   assert (value == "extended")
}

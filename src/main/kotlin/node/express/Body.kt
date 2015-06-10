package node.express

/**
 * A trait for different types of bodies
 */
interface Body: Map<String, Any?> {
  /**
   * Get the value of an attribute as its native type
   */
  fun get(key: String): Any?;

  /**
   * Get the value of an attribute as an integer
   */
  fun asInt(key: String): Int?;

  /**
   * Get the value of an attribute as a string
   */
  fun asString(key: String): String?;

  /**
   * Get the value of an element in a list as a integer
   */
  fun asInt(index: Int): Int?;

  /**
   * Get the value of an element in a list as a string
   */
  fun asString(index: Int): String?;

  /**
   * Get the native object that represents this body
   */
  fun asNative(): Any;
}
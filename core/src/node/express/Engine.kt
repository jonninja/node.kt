package node.express

/**
 * An Express rendering engine
 */
trait Engine {
  /**
   * Render a page
   * @param path a path to the template
   * @param data data that is passed to the page
   */
  fun render(path: String, data: Map<String, *>): String;
}
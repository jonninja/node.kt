package node.express

/**
 * A trait for classes to handle requests. An application can install handlers as either a
 * callback function or an object that implements the Handler trait.
 */
interface Handler {
  fun exec(req: Request, res: Response, next: () -> Unit)

  fun callback(): (req: Request, res: Response, next: () -> Unit) -> Unit {
    return { req, res, next ->
      this.exec(req, res, next)
    }
  }

  companion object {
    /**
     * Create a handler with a function callback suitable for middleware
     */
    fun middleware(callback: (req: Request, res: Response, next: () -> Unit) -> Unit): Handler {
      return NextFunHandler(callback)
    }
  }
}

/**
 * Handler that calls through to a function handler
 */
class NextFunHandler(val callback: (req: Request, res: Response, next: () -> Unit) -> Unit): Handler {
  override fun exec(req: Request, res: Response, next: () -> Unit) {
    this.callback(req, res, next)
  }
}
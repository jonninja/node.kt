package node.express.middleware

import node.express.Request
import node.express.Response
import node.express.Handler

/**
 * Middleware that parses bodies of various types and assigned to the body attribute of the request
 */
class BodyParser() : Handler {
  override fun exec(req: Request, res: Response, next: () -> Unit) {
    JsonBodyParser(req, res, {
      UrlEncodedBodyParser(req, res, next);
    });
  }
}
package pages

import node.express.Response
import node.express.Request

fun get(req: Request, res: Response) {
  res.send("Home")
}


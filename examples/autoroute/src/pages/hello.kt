package pages.hello

import node.express.Response
import node.express.Request

fun get(req: Request, res: Response) {
  res.send("Hello!")
}

fun _world(req: Request, res: Response) {
  res.send("""
  <html>
    <head>
    </head>
    <body>
      <h1>${req.param("world")}</h1>
    </body>
  </html>
  """)
}
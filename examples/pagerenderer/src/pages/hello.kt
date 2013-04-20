package pages.hello

import node.express.Response
import node.express.Request

fun index(req: Request, res: Response) {

}

fun world(req: Request, res: Response) {
  res.send("""
  <html>
    <head>
    </head>
    <body>
      <h1>Hello World!</h1>
    </body>
  </html>
  """)
}
package node.kt.examples.Static;

import node.express.Express
import node.express.middleware.LessCompiler
import node.express.middleware.FavIcon

/**
 * Example of using Node.kt to serve static files
 */
fun main(args: Array<String>) {
    var app = Express()
    app.use(FavIcon("public/yahoo.ico"))
    app.use(LessCompiler("public/"))
    app.use(app.static("public/"))
    app.listen(4000);
}

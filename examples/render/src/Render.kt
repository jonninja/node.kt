package node.kt.examples.Render

import node.express.Express

/**
 * Example app showing rendering of pages using different template engines
 */
fun main(args: Array<String>) {
    var app = Express()

    app["title"] = "My Kotlin App" // available in the template as settings.title
    app["view engine"] = "ftl"      // sets the default template engine

    app.get("/velocity", { req, res, next ->
        res.render("render.vm", hashMapOf(
                "name" to "Jon Nichols"
        ));
    })
    app.get("/freemarker", { req, res, next ->
        res.render("render", hashMapOf(
                "name" to "Jon Nichols"
        ));
    })

    app.listen(4000)
}
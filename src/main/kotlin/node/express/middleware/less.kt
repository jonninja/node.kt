package node.express.middleware

import java.io.File
import node.express.Response
import node.express.RouteHandler

/**
 * Compiles less files into CSS
 * @basePath the root path to look for less files relative to the request path
 */
public fun lessCompiler(basePath: String): RouteHandler.()->Unit {
    fun normalizePath(path: String): String {
        var newPath = path
        if (newPath.startsWith("/")) {
            newPath = newPath.substring(1)
        }
        if (newPath.endsWith("/")) {
            newPath = newPath.substring(0, newPath.length() - 1)
        }
        return newPath
    }

    val base = normalizePath(basePath)
    val cache = hashMapOf<String, File>()
    return {
        var path = req.param("*") as String
        var cssFile: File? = null

        if (path.endsWith(".less")) {
            if (!path.startsWith("/")) {
                path = "/" + path
            }

            var srcFile = File(base + path)

            if (!srcFile.exists()) {
                res.send(404)
            } else {
                cssFile = cache.get(path)
                if (cssFile != null && cssFile.exists()) {
                    // check to see if the srcFile has been changed, and force recompile
                    if (cssFile.lastModified() < srcFile.lastModified()) {
                        cssFile = null
                    }
                }
                if (cssFile == null) {
                    cssFile = File.createTempFile("LessCss", ".css")
                    var lessCompiler = org.lesscss.LessCompiler()
                    lessCompiler.compile(srcFile, cssFile)
                    cache.put(path, cssFile!!)
                }
            }
        }
        if (cssFile != null) {
            res.contentType("text/css")
            res.sendFile(cssFile)
        } else {
            next()
        }
    }
}
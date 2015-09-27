package node.util.io

import java.io.InputStream
import java.io.OutputStream

/**
 * Read through the input stream, sending data to the processor as it is read.
 */
fun InputStream.pipe(processor: (bytes: ByteArray, size: Int) -> Unit) {
    val bytes = ByteArray(1024)
    while (true) {
        val len = this.read(bytes)
        if (len < 0) {
            return
        }
        processor(bytes, len)
    }
}

/**
 * Pipe data from an input stream to an output stream
 */
fun InputStream.pipe(output: OutputStream) {
    this.pipe { bytes, size ->
        output.write(bytes, 0, size)
    }
    output.flush()
}
package node.template

import kotlin.test.assertEquals
import java.io.StringWriter
import java.io.Writer
import java.io.File
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.StringReader
import node.util.after
import node.util.until
import java.util.Stack

/**
 * 
 */
class KtTemplate(w: Writer) {
  val escapedQuote = "\"\"\""
  val writer = PrintWriter(w)
  val stack = Stack<State>()

  trait State {
    fun process(c: Char): Boolean
    fun end() {

    }
  }

  inner class Header(val writer: Appendable): State {
    {
      writer.append("import java.io.StringWriter\n");
    }

    val builder = StringBuilder()

    override fun process(c: Char): Boolean {
      if (c == '{') {
        builder.append(c)
        writer.append(builder.toString().replace("template ", "fun "))
        writer.append('\n')
        writer.append("val out = StringWriter();\n")
        stack.push(Function(writer))
        return true
      } else {
        builder.append(c)
        return true
      }
    }

    override fun end() {
      writer.append(builder)
    }
  }

  inner class Function(val writer: Appendable): State {
    var buffer = StringBuilder()
    override fun process(c: Char): Boolean {
      when (c) {
        '}' -> {
          writer.append("return out.toString();\n")
          writer.append(c)
          writer.append("\n")
          stack.pop()
        }
        '$' -> {
          stack.push(CodeStart(writer))
        }
        ' ' -> {
          buffer.append(c)
        }
        '\t' -> {
          buffer.append(c)
        }
        '\n' -> {
          // ignore
        }
        else -> {
          val o = Output(writer)
          buffer.append(c)
          buffer.toString().forEach { o.process(it) }
          buffer = StringBuilder()
          stack.push(o)
        }
      }
      return true
    }

    override fun end() {
      writer.append("\n")
    }
  }

  inner class Output(val writer: Appendable): State {
    {
      writer.append("out.write($escapedQuote")
    }

    override fun process(c: Char): Boolean {
      when (c) {
        '$' -> {
          writer.append("$escapedQuote)\n")
          stack.pop()
          return false
        }
        '\n' -> {
          writer.append("\n$escapedQuote)\n")
          stack.pop()
        }
        else -> {
          writer.append(c)
        }
      }
      return true
    }

    override fun end() {
      writer.append("\n$escapedQuote)\n")
    }
  }

  inner class CodeStart(val writer: Appendable): State {
    override fun process(c: Char): Boolean {
      if (c == '{') {
        stack.pop() // pop myself
        stack.push(BlockCode(writer))
      } else {
        stack.pop() // pop myself
        val inliner = InlineCode(writer)
        inliner.process(c)
        stack.push(inliner)
      }
      return true
    }
  }

  inner class InlineCode(val writer: Appendable): State {
    private var isStart = true

    override fun end() {
      writer.append("}())\n")
    }

    override fun process(c: Char): Boolean {
      if (isStart) {
        if (c.isJavaIdentifierStart()) {
          writer.append("out.write({")
          writer.append(c)
          isStart = false
        } else {
          end()
          stack.pop()
          return false
        }
      } else {
        if (c.isJavaIdentifierPart()) {
          writer.append(c)
        } else {
          end()
          stack.pop()
          return false
        }
      }
      return true
    }
  }

  inner class BlockCode(val writer: Appendable): State {
    {
      writer.append("out.write({")
    }

    override fun process(c: Char): Boolean {
      when (c) {
        '}' -> {
          writer.append("}())\n")
          stack.pop()
        }
        else -> {
          writer.append(c)
        }
      }
      return true
    }
  }

  fun process(text: String) {
    stack.push(Header(writer))
    val writer = StringWriter()
    for (c in text) {
      do {
        val top = stack.peek()
      } while (!(top!!.process(c)))
    }
    this.writer.println(writer.toString())
  }
}

fun processFile(file: File) {
  val writer = StringWriter()
  val outFile = File(file.getParent(), "${file.name.until(".")}.kt")

  val tmpl = KtTemplate(writer)
  tmpl.process(file.readText())
  outFile.writeText(writer.getBuffer().toString())
}

fun main(args: Array<String>) {
  processFile(java.io.File(args[0]))
}
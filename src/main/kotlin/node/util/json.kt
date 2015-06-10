package node.util.json

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import com.fasterxml.jackson.databind.JsonNode
import java.util.ArrayList
import java.lang.reflect.ParameterizedType
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.databind.node.IntNode
import java.util.LinkedHashMap
import node.util.konstructor
import node.util.after
import node.util.until
import com.fasterxml.jackson.databind.node.NullNode
import java.util.HashMap
import node.util.field
import java.io.InputStream
import com.fasterxml.jackson.databind.node.LongNode
import node.util._withNotNull

/**
 * Utilities for working with JSON
 */
private object JSONHelpers {
  val json = ObjectMapper()
}
//private val json = ObjectMapper()
private val stringClass = javaClass<String>()
private val intClass = javaClass<Int>()
private val longClass = javaClass<Long>()
private val anyClass = javaClass<Any>()

/**
 * Parse a JSON string into a JsonNode. Uses default parsing options
 */
fun String.json(): Any {
  return JSONHelpers.json.readValue(this, javaClass<Any>())!!
}

public fun <T> String.json(dataClass: Class<T>): T {
  val node = JSONHelpers.json.readValue(this, javaClass<JsonNode>())
  return node!!.convert<T, Nothing>(dataClass)!!
}


/**
 * Convert an object to a JSON string
 */
fun Any.toJsonString(): String {
  return JSONHelpers.json.writeValueAsString(this)!!
}

fun Any.asMap(vararg fields: String): Map<String, Any> {
  val result = HashMap<String, Any>()

  fields.forEach {
    val fieldName = it
    _withNotNull(this.field(it)) {
      result.put(fieldName, it)
    }
  }

  return result
}

/**
 * Construct a map suitable for json construction
 */
fun JSONMap(): MutableMap<String, Any> {
  return HashMap()
}

/**
 * Read the data from a file as JSON
 */
fun File.json(): Any {
  return JSONHelpers.json.readValue(this, javaClass<Map<String, Any?>>())!!
}

fun InputStream.json(): Any {
  return JSONHelpers.json.readValue(this, javaClass<Any>())!!
}

fun <T> InputStream.json(dataClass: Class<T>): T {
  return JSONHelpers.json.readValue(this, dataClass)!!
}

/**
 * Read the data from a file as JSON
 */
fun <T> File.json(dataClass: Class<T>): T {
  return JSONHelpers.json.readValue(this, dataClass)!!
}

private fun IntNode.intValue(): Int {
  return this.intValue()
}

private fun TextNode.textValue(): String? {
  return this.textValue()
}

/**
 * Convert a text node to a given type. Automatically converts to string or
 * Int. If another type is given, looks for a constructor with only a string.
 */
@suppress("UNCHECKED_CAST")
private fun <T:Any> convertTextNode(tn: TextNode, aType: Class<T>): T? {
  val result:Any? = when (aType) {
    stringClass -> tn.textValue()
    intClass -> tn.textValue()!!.toInt()
    anyClass -> tn.textValue()
    else -> {
      aType.getConstructor(stringClass).newInstance(tn.textValue())
    }
  }
  return result as T?
}

/**
 * Convert an IntNode to a target type.
 */
@suppress("UNCHECKED_CAST")
private fun <T:Any> convertIntNode(node: IntNode, target: Class<T>): T? {
  val result: Any? = when (target) {
    stringClass -> node.intValue().toString()
    intClass -> node.intValue()
    anyClass -> node.intValue()
    else -> target.getConstructor(intClass).newInstance(node.intValue())
  }
  return result as T?
}

/**
 * Convert an IntNode to a target type.
 */
@suppress("UNCHECKED_CAST")
private fun <T:Any> convertLongNode(node: LongNode, target: Class<T>): T? {
  val result: Any? = when (target) {
    stringClass -> node.longValue().toString()
    longClass -> node.longValue()
    anyClass -> node.longValue()
    else -> target.getConstructor(intClass).newInstance(node.longValue())
  }
  return result as T?
}

@suppress("UNCHECKED_CAST", "BASE_WITH_NULLABLE_UPPER_BOUND")
fun <T, K> JsonNode.convert(ty: Class<T>, sub: Class<K>? = null): T? {
  val result: Any? = when {
    this is TextNode -> convertTextNode(this, ty)
    this is IntNode -> convertIntNode(this, ty)
    this is LongNode -> convertLongNode(this, ty)
    this is NullNode -> {
      null
    }
    this.isArray() -> {
      when {
        ty == javaClass<Any>() || javaClass<List<*>>().isAssignableFrom(ty) -> {
          @suppress("CAST_NEVER_SUCCEEDS")
          var subType: Class<Any>? = sub as Class<Any>?
          val l: MutableList<Any?> = (if (sub != null) {
            ArrayList<K>() as MutableList<Any?>
          } else if (ty.getGenericSuperclass() != null){
            subType = (ty.getGenericSuperclass() as ParameterizedType).getActualTypeArguments()!![0] as Class<Any>
            ty.newInstance() as MutableList<Any?>
          } else {
            subType = javaClass<Any>()
            ArrayList<K>() as MutableList<Any?>
          })

          for (el in this) {
            @suppress("CAST_NEVER_SUCCEEDS")
            val subValue = el.convert<K, Nothing>(subType as Class<K>)
            l.add(subValue!!)
          }
          l
        }
        else -> null
      }
    }
    this.isObject() -> {
      if (ty == javaClass<Any>() || ty == javaClass<Map<*,*>>()) {
        val valueType = if (sub != null) sub else javaClass<Any>()
        val result = LinkedHashMap<String, Any?>()
        for (field in this.fields()) {
          result.put(field.key, field.value.convert<Any,Nothing>(valueType as Class<Any>))
        }
        result
      } else if (javaClass<Map<String,*>>().isAssignableFrom(ty)) {
        val subType = (ty.getGenericSuperclass() as ParameterizedType).getActualTypeArguments()!![1] as Class<Any?>
        val result = ty.newInstance() as MutableMap<String,Any?>
        for (field in this.fields()) {
          result.put(field.key, field.value.convert<Any?,Nothing>(subType))
        }
        result
      } else {
        val node = this

        ty.konstructor().newInstance {
          val nodeValue = node.get(it.name)
          if (nodeValue == null) null else
          {
            if (it.jType == javaClass<List<*>>()) {
              // for lists, use some special casing and get the content type from the JetValueParameter
              val componentTypeName = it.kType.replace("/".toRegex(), ".").after("<").until(";").substring(1)
              val componentType = Class.forName(componentTypeName)
              nodeValue.convert(it.jType as Class<Any>, componentType as Class<Any>)
            } else if (it.jType == javaClass<Map<*,*>>()) {
              val components = it.kType.replace("/".toRegex(), ".").after("<").until(">").split(";".toRegex()).toTypedArray().map { it.substring(1) }
              val componentType = Class.forName(components[1])
              nodeValue.convert(it.jType as Class<Any>, componentType as Class<Any>)
            } else {
              nodeValue.convert<Any?, Nothing>(it.jType as Class<Any?>, null)
            }
          }
        }
      }
    }
    else -> {
      null
    }

  }
  return result as T?
}
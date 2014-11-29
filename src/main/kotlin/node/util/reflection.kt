package node.util

import node.express.MissingParameterException
import java.util.ArrayList
import java.lang.reflect.Constructor
import java.util.HashMap
import jet.runtime.typeinfo.JetValueParameter
import java.lang.reflect.Method


/**
 * Some reflection APIs
 */
fun Any.method(name: String): ((vararg v: Any?) -> Any?)? {
  var methods = this.javaClass.getMethods();
  for (m in methods) {
    if (m.getName() == name) {
      return { (vararg v : Any?):Any? ->
        m.invoke(this, *v)
      }
    }
  }
  return null;
}

/**
 * Get a collection of methods of a class as a collection.
 */
val <T> Class<T>.methods: Collection<Method>
  get() {
    return this.getMethods().toList()
  }

fun <T> Any.dynamic(name: String, vararg v: Any?):Any? {
  return this.javaClass.methods.firstOrNull { it.getName() == name }!!.invoke(this, *v)
}

/**
 * Get the value of a  field
 */
fun Any.field(name: String): Any? {
  try {
    var f = this.javaClass.getField(name);
    return f.get(this);
  } catch (t: Throwable) {
    return null;
  }
}

[suppress("UNCHECKED_CAST")]
class Property<T>(val obj: Any, name: String) {
  private val field = obj.javaClass.getField(name)
  var value:T
    get() { return field.get(obj) as T }
    set(t: T) { field.set(obj, t) }
}

fun Any.property<T>(name: String): Property<T> {
  return Property(this, name)
}

[suppress("UNCHECKED_CAST")]
data class KotlinConstructor<T>(val jet: Constructor<T>, val def: Constructor<T>?) {
  class object {
    private var dataConstructors: MutableMap<Class<*>, KotlinConstructor<*>>? = null

    fun <T> get(ty: Class<T>): KotlinConstructor<T> {
      if (dataConstructors == null) {
        dataConstructors = HashMap<Class<*>, KotlinConstructor<*>>()
      }

      // find the constructor with the JetValueParameter annotations
      var constructor = ty.getConstructors().firstOrNull {
        if (it.getParameterTypes()!!.size > 0) {
          it.getParameterAnnotations()[0]!!.firstOrNull {
            it is JetValueParameter
          } != null
        } else {
          false
        }
      }
      if (constructor == null) {
        constructor = ty.getConstructor()
      }

      // next, find the constructor that matches in case there are default types
      val types = constructor!!.getParameterTypes()!!
      val extended = Array<Class<out Any?>>(types.size + 1, { index->
        if (index < (types.size)) types[index] else javaClass<Int>()
      })
      val defCon = {
        try {
          ty.getConstructor(*extended)
        } catch (t: Throwable) {
          null
        }
      }()
      return KotlinConstructor(constructor as Constructor<T>, defCon)
    }
  }

  // Describes a constructor parameter
  data class Parameter(val name: String, val kType: String, val jType: Class<out Any?>, val annotations: List<Annotation>) {
    fun <T:Any> getAnnotation(annotationType: Class<T>): T? {
      return annotations.firstOrNull {
        it.annotationType() == annotationType
      } as? T?
    }
  }

  // Construct a new instance from a set of named parameters
  fun newInstance(parameters: Map<String, Any?>): T {
    return newInstance({
      parameters.get(it.name) })
  }

  fun newInstance(p: (param: Parameter)->Any?): T {
    var bitmask = 0
    var annotations = jet.getParameterAnnotations()
    val types = jet.getParameterTypes()!!
    val missing = ArrayList<String>()
    val parameters = (0..annotations.size - 1).mapTo(java.util.ArrayList<Any?>()) { index ->
      val jetParam = annotations[index]!!.firstOrNull { it is JetValueParameter }!! as JetValueParameter
      val value = p(Parameter(jetParam.name(), jetParam.`type`(), types[index], annotations[index]!!.toList()))
      if (value == null) {
        if (false/*jetParam.hasDefaultValue()*/) {
          bitmask += Math.pow(2.0, index.toDouble()).toInt()
          value
        } else if (jetParam.`type`().startsWith('?')) {
          value
        } else {
          missing.add(jetParam.name())
          ""
        }
      } else {
        value
      }
    }
    if (missing.size > 0) {
      throw MissingParameterException(missing)
    }
    return if (def != null) {
      parameters.add(bitmask)
      def.newInstance(*(parameters.toArray()))!!
    } else {
      jet.newInstance(*(parameters.toArray()))!!
    }
  }
}

/**
 * Get a Kotlin constructor for a class. A Kotlin constructor takes care of default
 * values.
 */
public fun <T> Class<T>.konstructor(): KotlinConstructor<T> {
  return KotlinConstructor.get(this)
}

class MissingParameterException(vararg parameters: String) : IllegalArgumentException(parameters.joinToString())

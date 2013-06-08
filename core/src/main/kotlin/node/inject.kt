package node.inject

import node.util.konstructor
import node.util._with
import node.*
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import node.inject.Registry.BindingReceiver
import java.util.HashMap

enum class CacheScope {
  GLOBAL
  OPERATION
  SESSION
}

/**
 * Creates and manages instances of bound classes. Instances are cached based on the CacheScope that
 * is bound to the instance. For example, if a class is bound with an OPERATION scope, a Factory with that
 * scope will cache the instance.
 */
class Factory(val registry: Registry, val scope: CacheScope, val parent: Factory? = null) {
  val cache = HashMap<String, Any?>()

  /**
   * Get an instance of the given class, optionally bound to the given name.
   */
  fun instanceOf<T>(clazz: Class<T>, name: String? = null): T {
    if (clazz == javaClass<Factory>()) {
      return this as T;
    }
    if (clazz == javaClass<Registry>()) {
      return registry as T;
    }

    val binding = registry.getBinding(clazz, name)
    if (binding != null) {
      if (binding.scope == null) {
        return binding.binding.instance(this, name)
      } else if (binding.scope == this.scope) {
        if (cache.containsKey(bindingKey(clazz, name))) {
          return cache.get(bindingKey(clazz, name)) as T
        } else {
          return synchronized(binding) {
            _with (binding.binding.instance(this, name)) {
              cache.put(bindingKey(clazz, name), it)
            }
          }
        }
      } else if (parent != null) {
        return parent.instanceOf(clazz, name)
      } else {
        // if the type is bound to a scope, but it is not this one, we have a problem
        throw IllegalStateException("Unable to create ${clazz.getCanonicalName()} in the scope that it was bound to. Make sure you are using a factory with the correct cache scope.")
      }
    }
    throw NotFoundException("No binding found for ${bindingKey(clazz, name)}")
  }

  /**
   * Install an instance into the Factory. Useful if you have objects that can't be instantiated
   * with the standard bindings that you need to make available to bound instances.
   */
  fun install<T>(instance: T, clazz: Class<T>, name: String? = null) {
    cache.put(bindingKey(clazz, name), instance)
  }
}

fun bindingKey<T>(src: Class<T>, name: String? = null): String {
  return "${src.getCanonicalName()}${if (name != null) name else ""}"
}

/**
 * A registry of class bindings to be used by the factory.
 */
open class Registry(val parentRegistry: Registry? = null) {
  val bindings = hashMapOf<String, BindingReceiver<*>>()
  val globalFactory = Factory(this, CacheScope.GLOBAL, null)

  /**
   * Bind a class to the registry. The binding is not complete until one of the methods
   * of the BindingReciever is called: ie. bind(String.class).to("foo")
   */
  fun <T> bind(src: Class<T>, name: String? = null): BindingReceiver<T> {
    return _with(BindingReceiver<T>(src, name)) {
      bindings.put("${src.getCanonicalName()}${if (name != null) name else ""}", it)
    }
  }

  fun get(key: Any): Binding<*>? {
    return bindings[key]?.binding
  }

  public class BindingReceiver<T>(val src: Class<T>, val name: String? = null) {
    var binding: Binding<T> = ConstructorBinding(src)
    var scope: CacheScope? = null
    var cached: T? = null

    /**
     * Bind a class to a key. The class's constructor will be called to create the instance when the key
     * is requested.
     */
    fun <I:T> toImplementation(target: Class<I>): BindingReceiver<T> {
      binding = ConstructorBinding(target)
      return this;
    }

    /**
     * Bind an instance to a key.
     */
    fun <I:T> toInstance(target: I): BindingReceiver<T> {
      binding = InstanceBinding(target)
      return this
    }

    fun to(any: Any?) {
      throw IllegalArgumentException("This is probably not what you want! Use toInstance to map to an instance")
    }

    /**
     * Bind a callback to a key.
     */
    fun toFactory(target: (factory: Factory, name: String?)->T): BindingReceiver<T> {
      binding = LiteralBinding(target)
      return this
    }

    fun withScope(scope: CacheScope): BindingReceiver<T> {
      this.scope = scope
      return this
    }
  }

  open fun getBinding<T>(clazz: Class<T>, name: String? = null): BindingReceiver<T>? {
    val key = bindingKey(clazz, name)
    var result = bindings[key] as? BindingReceiver<T> ?: parentRegistry?.getBinding(clazz, name)
    if (result == null && name != null) {
      result = getBinding(clazz) // get a binding without a name
    }
    return result
  }

  /**
   * Get a factory for this registry, bound to the given cache scope.
   */
  public fun factory(scope: CacheScope? = null): Factory {
    if (scope == null || scope == CacheScope.GLOBAL) {
      return globalFactory
    } else {
      return Factory(this, scope, globalFactory)
    }
  }
}

/**
 * A registry that reads instances from Configuration
 */
class ConfigurationRegistry: Registry() {
  override fun <T> getBinding(clazz: Class<T>, name: String?): Registry.BindingReceiver<T>? {
    if (name != null) {
      if (Configuration.get(name) != null) {
        return BindingReceiver(clazz, name).toInstance(Configuration.get(name) as T)
      }
    }
    return super<Registry>.getBinding(clazz, name)
  }
}

private trait Binding<T> {
  fun instance(factory: Factory, name: String?): T
}

private class ConstructorBinding<T,I:T>(val clazz: Class<I>): Binding<T> {
  override fun instance(factory: Factory, name: String?): T {
    return clazz.konstructor().newInstance({ p ->
      val name = p.getAnnotation(javaClass<named>())?.key ?: p.name
      factory.instanceOf(p.jType as Class<T>, name)
    })
  }
}

private class InstanceBinding<T>(val instance: T): Binding<T> {
  override fun instance(factory: Factory, name: String?): T {
    return instance
  }
}

private class LiteralBinding<T>(val factory: (factory: Factory, name: String?)->T): Binding<T> {
  override fun instance(factory: Factory, name: String?): T {
    return factory(factory, name)
  }
}
/**
 * Key annotation. Allows for annotating a parameter such that the injected instance will
 * relate to that key. Useful for when you want to have available different versions of the
 * same type.
 */
Retention(RetentionPolicy.RUNTIME)
annotation class named(val key: String)
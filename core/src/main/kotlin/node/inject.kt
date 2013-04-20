package node.inject

import node.util.konstructor
import node.NotFoundException
import node.util.with
import node.util._if
import node.Configuration
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * An injection registry.
 */
class Registry {
  val bindings = hashMapOf<Any, BindingReceiver<*>>()

  /**
   * Bind a class to the registry. The binding is not complete until one of the methods
   * of the BindingReciever is called: ie. bind(String.class).to("foo")
   */
  fun <T> bind(src: Class<T>): BindingReceiver<T> {
    return with(BindingReceiver<T>(src)) {
      bindings.put(src, it)
    }
  }

  /**
   * Bind a key to the registry. Used in conjunction with the 'named' annotation
   */
  fun bind(key: String): BindingReceiver<Any?> {
    return with(BindingReceiver<Any?>(key)) {
      bindings.put(key, it)
    }
  }

  fun get(key: Any): Binding<*>? {
    return bindings[key]?.binding
  }

  inner open class BindingReceiver<T>(val src: Any) {
    var binding: Binding<T>? = {
      if (src is Class<*>) {
        ConstructorBinding(src as Class<T>)
      } else {
        InstanceBinding(src) as Binding<T>
      }
    }()

    /**
     * Bind a class to a key. The class's constructor will be called to create the instance when the key
     * is requested.
     */
    open fun <I:T> toImplementation(target: Class<I>) {
      binding = ConstructorBinding(target)
    }

    /**
     * Bind an instance to a key.
     */
    open fun <I:T> toInstance(target: I) {
      binding = InstanceBinding(target)
    }

    fun to(any: Any?) {
      throw IllegalArgumentException("This is probably not what you want! Use toInstance to map to an instance")
    }

    /**
     * Bind a callback to a key.
     */
    open fun toFactory(target: (Scope)->T) {
      binding = LiteralBinding(target)
    }

    /**
     * Bind a singleton class to a key.
     */
    open fun <I:T> toSingleton(implementation: Class<I>) {
      binding = SingletonBinding(ConstructorBinding(implementation))
    }

    open fun toSingletonFactory(factory: (Scope)->T) {
      binding = SingletonBinding(LiteralBinding(factory))
    }
  }
}

/**
 * An injection scope does the work of actualling providing object instances for injection keys.
 * Bindings are read from the registry and used to determine instances to return.
 * Each scope will have its own rules for determining when to create new objects vs reusing
 * previously created objects.
 */
trait Scope {
  val registry: Registry

  /**
   * Get an instance related to the registered key.
   */
  fun instance<T>(key: Any): T

  /**
   * Specialized version of instance when the key is a class, allowing for generics to
   * cast the result.
   */
  fun instanceOf<T>(key: Class<T>, name: String? = null): T
}

/**
 * The default scope creates instances on demand. There is no caching of objects, so each
 * time a request is made, a new instance is created.
 */
open class DefaultScope(override val registry: Registry): Scope {
  override fun instance<T>(key: Any): T {
      return with(registry[key]?.instance(this)) {
        if (it == null) throw NotFoundException()
      }!! as T
  }

  /**
   * Get an instance of a class. If no implementation is registered,
   * attempts to inject the class itself.
   */
  override fun <T> instanceOf(key: Class<T>, name: String?): T {
    val binding = registry[key]
    return if (binding != null) {
      binding.instance(this) as T
    } else {
      ConstructorBinding(key).instance(this)
    }
  }
}

/**
 * A cached scope reuses all objects. Typically, this scope will be used for short lived operations,
 * such as a server request.
 */
class CachedScope(registry: Registry): DefaultScope(registry) {
  val instances = hashMapOf<Any, Any?>()

  override fun instance<T>(key: Any): T {
    return instances.getOrElse(key, {
      with (registry[key]?.instance(this)) {
        if (it == null) throw NotFoundException("Unable to find instance for $key")
      }
    }) as T
  }

  override fun <T> instanceOf(key: Class<T>, name: String?): T {
    return instance<T>(key:Any)
  }

  fun registerInstance<T>(key: Class<T>, instance: T) {
    instances.put(key, instance)
  }
}

private trait Binding<T> {
  fun instance(scope: Scope): T
}

private class ConstructorBinding<T,I:T>(val clazz: Class<I>): Binding<T> {
  override fun instance(scope: Scope): T {
    return clazz.konstructor().newInstance({ p ->
      val name = p.getAnnotation(javaClass<key>())?.key ?: p.name
      _if (p.getAnnotation(javaClass<key>())) {
        scope.instance<Any>(it.key)
      } ?: _if (p.getAnnotation(javaClass<configuration>())) {
        Configuration.get(it.name)
      } ?: scope.instanceOf(p.jType as Class<T>);
    })
  }
}

private class SingletonBinding<T>(val binding: Binding<T>): Binding<T> {
  private var singleton: T? = null

  override fun instance(scope: Scope): T {
    if (singleton == null) {
      singleton = binding.instance(scope)
    }
    return singleton!!
  }
}

private class InstanceBinding<T>(val instance: T): Binding<T> {
  override fun instance(scope: Scope): T {
    return instance
  }
}

private class LiteralBinding<T>(val factory: (Scope)->T): Binding<T> {
  override fun instance(scope: Scope): T {
    return factory(scope)
  }
}

/**
 * Key annotation. Allows for annotating a parameter such that the injected instance will
 * relate to that key. Useful for when you want to have available different versions of the
 * same type.
 */
Retention(RetentionPolicy.RUNTIME)
annotation class key(val key: Any)

/**
 * Configuration annotation. The value is set to the configuration value found at the provided
 * path.
 */
Retention(RetentionPolicy.RUNTIME)
annotation class configuration(val name: String)
package node

// A collection of exceptions that are useful accross a wide range
// of applications. The JDK doesn't really provide standard exceptions, and seems to
// want developers to create custom exceptions for almost every case. These exceptions
// are meant to apply to a class of the same types of issues that seem to come up
// regularly during development: something isn't found, there's a duplicate, etc.

/**
 * Exception to throw when something can't be found. Examples include a database lookup,
 * REST call that returns a 404, etc.
 */
class NotFoundException(msg: String? = null, cause: Throwable? = null): Exception(msg, cause)

/**
 * Exception to throw when an illegal duplicate is encountered.
 */
class DuplicateException(msg: String? = null, key: String? = null, cause: Throwable? = null): Exception(msg)
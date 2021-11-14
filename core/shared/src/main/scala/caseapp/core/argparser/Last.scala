package caseapp.core.argparser

/** Allows an argument to be specified multiple times.
  *
  * Discards previously specified values.
  *
  * @see
  *   [[caseapp.core.argparser.LastArgParser]]
  *
  * @param value:
  *   actual value of type [[T]]
  * @tparam T:
  *   wrapped type
  */
final case class Last[T](value: T) // extends AnyVal // having issues since Scala Native 0.4.1

package caseapp.core.argparser

/** Allows an argument to be specified multiple times.
  *
  * Discards previously specified values: the last one wins. Without `Last`, specifying an option
  * more than once is an error (see [[caseapp.core.Error.AlreadySpecified]]).
  *
  * {{{
  * case class Options(level: Last[Int] = Last(0))
  * // "--level 1 --level 2" gives level = Last(2)
  * }}}
  *
  * @see
  *   [[caseapp.core.argparser.LastArgParser]]
  *
  * @param value:
  *   actual value of type `T`
  * @tparam T:
  *   wrapped type
  */
final case class Last[T](value: T) // extends AnyVal // having issues since Scala Native 0.4.1

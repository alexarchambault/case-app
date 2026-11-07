package caseapp.core

import caseapp.core.argparser.{ArgParser, Consumed}

/** A value, along with the position of the argument(s) it was parsed from
  *
  * Use `Indexed[T]` as a field type, rather than `T`, to know where the corresponding option was
  * passed in the arguments (for example, to know the relative order of several options).
  *
  * {{{
  * case class Options(foo: Indexed[String] = Indexed("default"))
  * // "other --foo value" gives foo = Indexed(1, 2, "value")
  * }}}
  *
  * @param index
  *   position of the option in the arguments (starting from 0), or -1 if unknown (like for default
  *   values)
  * @param length
  *   number of arguments the value was parsed from: 2 for `--foo value`, 1 for `--foo=value` or for
  *   flags (`--foo`), 0 if unknown
  * @param value
  *   actual value
  */
final case class Indexed[+T](
  index: Int,
  length: Int,
  value: T
)

object Indexed {

  /** A value with no known position (index -1, length 0), handy for default values */
  def apply[T](value: T): Indexed[T] =
    Indexed(-1, 0, value)

  /** Indexes the elements of `seq`, each spanning one argument, starting at `startIdx` */
  def list[T](seq: List[T], startIdx: Int): List[Indexed[T]] =
    seq.zipWithIndex.map {
      case (elem, idx) =>
        Indexed(startIdx + idx, 1, elem)
    }

  /** Indexes the elements of `seq`, each spanning one argument, starting at `startIdx` */
  def seq[T](seq: Seq[T], startIdx: Int): Seq[Indexed[T]] =
    seq.zipWithIndex.map {
      case (elem, idx) =>
        Indexed(startIdx + idx, 1, elem)
    }

  /** Parses an `Indexed[T]` with the parser of `T`, keeping track of the argument position */
  implicit def argParser[T: ArgParser]: ArgParser[Indexed[T]] =
    new ArgParser[Indexed[T]] {
      private val underlying = ArgParser[T]
      def apply(current: Option[Indexed[T]], index: Int, span: Int, value: String) =
        underlying(current.map(_.value), index, span, value)
          .map(t => Indexed(index, span, t))
      override def apply(current: Option[Indexed[T]], index: Int) =
        underlying(current.map(_.value), index)
          .map(t => Indexed(index, 1, t))
      override def optional(current: Option[Indexed[T]], index: Int, span: Int, value: String) = {
        val (consumed, res) = underlying.optional(current.map(_.value), index, span, value)
        val len             = if (consumed.value) span else 1
        (consumed, res.map(t => Indexed(index, len, t)))
      }
      override def isFlag = underlying.isFlag
      def description     = underlying.description
    }
}

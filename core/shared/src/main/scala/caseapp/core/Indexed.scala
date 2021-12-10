package caseapp.core

import caseapp.core.argparser.{ArgParser, Consumed}

final case class Indexed[+T](
  index: Int,
  length: Int,
  value: T
)

object Indexed {

  def apply[T](value: T): Indexed[T] =
    Indexed(-1, 0, value)

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

package caseapp.core.complete

import caseapp.core.help.{WithFullHelp, WithHelp}
import caseapp.core.{Arg, RemainingArgs}

trait Completer[-T] { self =>
  def optionName(prefix: String, state: Option[T], args: RemainingArgs): List[CompletionItem]
  def optionValue(
    arg: Arg,
    prefix: String,
    state: Option[T],
    args: RemainingArgs
  ): List[CompletionItem]
  def argument(prefix: String, state: Option[T], args: RemainingArgs): List[CompletionItem]

  def postDoubleDash(state: Option[T], args: RemainingArgs): Option[Completer[T]] =
    None

  final def contramapOpt[U](f: U => Option[T]): Completer[U] =
    Completer.Mapped(this, f)
  final def withHelp: Completer[WithHelp[T]] =
    contramapOpt(_.baseOrError.toOption)
  final def withFullHelp: Completer[WithFullHelp[T]] =
    contramapOpt(_.withHelp.baseOrError.toOption)
}

object Completer {

  implicit class CompleterOps[T](private val completer: Completer[T]) extends AnyVal {
    def completeOptionValue(f: (
      Arg,
      String,
      Option[T],
      RemainingArgs
    ) => Option[List[CompletionItem]]): Completer[T] =
      WithOptionValue(completer, f)
  }

  class DefaultTo[-T](default: Completer[T]) extends Completer[T] {
    override def optionName(
      prefix: String,
      state: Option[T],
      args: RemainingArgs
    ): List[CompletionItem] =
      default.optionName(prefix, state, args)
    override def optionValue(
      arg: Arg,
      prefix: String,
      state: Option[T],
      args: RemainingArgs
    ): List[CompletionItem] =
      default.optionValue(arg, prefix, state, args)
    override def argument(
      prefix: String,
      state: Option[T],
      args: RemainingArgs
    ): List[CompletionItem] =
      default.argument(prefix, state, args)
    override def postDoubleDash(state: Option[T], args: RemainingArgs): Option[Completer[T]] =
      default.postDoubleDash(state, args)

    override def toString(): String =
      s"Completer.DefaultTo($default)"
  }

  private final case class WithOptionValue[T](
    self: Completer[T],
    f: (Arg, String, Option[T], RemainingArgs) => Option[List[CompletionItem]]
  ) extends DefaultTo[T](self) {
    override def optionValue(
      arg: Arg,
      prefix: String,
      state: Option[T],
      args: RemainingArgs
    ): List[CompletionItem] =
      f(arg, prefix, state, args).getOrElse {
        super.optionValue(arg, prefix, state, args)
      }
  }

  private final case class Mapped[T, U](self: Completer[T], f: U => Option[T])
      extends Completer[U] {
    def optionName(prefix: String, state: Option[U], args: RemainingArgs): List[CompletionItem] =
      self.optionName(prefix, state.flatMap(f), args)
    def optionValue(
      arg: Arg,
      prefix: String,
      state: Option[U],
      args: RemainingArgs
    ): List[CompletionItem] =
      self.optionValue(arg, prefix, state.flatMap(f), args)
    def argument(prefix: String, state: Option[U], args: RemainingArgs): List[CompletionItem] =
      self.argument(prefix, state.flatMap(f), args)

    override def postDoubleDash(state: Option[U], args: RemainingArgs): Option[Completer[U]] =
      self.postDoubleDash(state.flatMap(f), args).map(_.contramapOpt(f))
  }
}

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

  def contramapOpt[U](f: U => Option[T]): Completer[U] =
    new Completer[U] {
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
  def withHelp: Completer[WithHelp[T]] =
    contramapOpt(_.baseOrError.toOption)
  def withFullHelp: Completer[WithFullHelp[T]] =
    contramapOpt(_.withHelp.baseOrError.toOption)
}

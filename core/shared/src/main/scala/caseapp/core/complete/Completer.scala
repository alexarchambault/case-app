package caseapp.core.complete

import caseapp.core.Arg
import caseapp.core.help.{WithFullHelp, WithHelp}

trait Completer[T] { self =>
  def optionName(prefix: String, state: Option[T]): List[CompletionItem]
  def optionValue(arg: Arg, prefix: String, state: Option[T]): List[CompletionItem]
  def argument(prefix: String, state: Option[T]): List[CompletionItem]

  def contramapOpt[U](f: U => Option[T]): Completer[U] =
    new Completer[U] {
      def optionName(prefix: String, state: Option[U]): List[CompletionItem] =
        self.optionName(prefix, state.flatMap(f))
      def optionValue(arg: Arg, prefix: String, state: Option[U]): List[CompletionItem] =
        self.optionValue(arg, prefix, state.flatMap(f))
      def argument(prefix: String, state: Option[U]): List[CompletionItem] =
        self.argument(prefix, state.flatMap(f))
    }
  def withHelp: Completer[WithHelp[T]] =
    contramapOpt(_.baseOrError.toOption)
  def withFullHelp: Completer[WithFullHelp[T]] =
    contramapOpt(_.withHelp.baseOrError.toOption)
}

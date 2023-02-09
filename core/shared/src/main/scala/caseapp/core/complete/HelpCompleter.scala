package caseapp.core.complete

import caseapp.core.help.Help
import caseapp.core.{Arg, RemainingArgs}

class HelpCompleter[T](help: Help[T]) extends Completer[T] {
  def optionName(prefix: String, state: Option[T], args: RemainingArgs): List[CompletionItem] =
    help
      .args
      .iterator
      .flatMap { arg =>
        val names = (arg.name +: arg.extraNames)
          .map(help.nameFormatter.format)
          .map(n => (if (n.length == 1) "-" else "--") + n)
          .filter(_.startsWith(prefix))
        if (names.isEmpty) Iterator.empty
        else
          Iterator(CompletionItem(names.head, arg.helpMessage.map(_.message), names.tail))
      }
      .toList
  def optionValue(
    arg: Arg,
    prefix: String,
    state: Option[T],
    args: RemainingArgs
  ): List[CompletionItem] =
    Nil
  def argument(prefix: String, state: Option[T], args: RemainingArgs): List[CompletionItem] =
    Nil
}

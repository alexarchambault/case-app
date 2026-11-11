package caseapp.core.complete

import caseapp.Name
import caseapp.core.help.Help
import caseapp.core.util.Formatter
import caseapp.core.util.NameOps.toNameOps
import caseapp.core.{Arg, RemainingArgs}

/** Default [[Completer]], completing option names from `help`
  *
  * Option names are completed along with the help message of the corresponding argument. Option
  * values and non-option arguments aren't completed.
  */
class HelpCompleter[T](help: Help[T]) extends Completer[T] {
  def optionName(prefix: String, state: Option[T], args: RemainingArgs): List[CompletionItem] =
    HelpCompleter.optionNames(help.args, help.nameFormatter, prefix)
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

object HelpCompleter {

  /** Completion items for the names of `args` starting with `prefix`
    *
    * Items of hidden arguments (see [[caseapp.Hidden]]) are marked as hidden, and have a
    * description starting with "(hidden)" (see [[CompletionItem.dropHidden]]).
    */
  def optionNames(
    args: Seq[Arg],
    nameFormatter: Formatter[Name],
    prefix: String
  ): List[CompletionItem] =
    args
      .iterator
      .flatMap { arg =>
        val names = arg.names
          .map(_.option(nameFormatter))
          .filter(_.startsWith(prefix))
        if (names.isEmpty) Iterator.empty
        else {
          val description = arg.helpMessage.map(_.message)
          val item =
            if (arg.noHelp)
              CompletionItem(
                names.head,
                CompletionItem.hiddenDescription(description),
                names.tail,
                hidden = true
              )
            else
              CompletionItem(names.head, description, names.tail)
          Iterator(item)
        }
      }
      .toList
}

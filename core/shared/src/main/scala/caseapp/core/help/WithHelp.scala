package caseapp.core.help

import caseapp.{ExtraName, Group, Help, HelpMessage, Parser, Recurse}
import caseapp.core.Error

/** Helper to add `--usage` and `--help` options to an existing type `T`.
  *
  * @param usage:
  *   whether usage was requested
  * @param help:
  *   whether help was requested
  * @param baseOrError:
  *   parsed `T` in case of success, or error message else
  * @tparam T:
  *   type to which usage and help options are added
  */
final case class WithHelp[+T](
  @Group("Help")
  @HelpMessage("Print usage and exit")
  usage: Boolean = false,
  @Group("Help")
  @HelpMessage("Print help message and exit")
  @ExtraName("h")
  help: Boolean = false,
  @Recurse
  baseOrError: Either[Error, T]
) {
  def map[U](f: T => U): WithHelp[U] =
    copy(baseOrError = baseOrError.map(f))
}

object WithHelp extends WithHelpCompanion

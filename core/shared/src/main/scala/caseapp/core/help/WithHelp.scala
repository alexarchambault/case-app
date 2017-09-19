package caseapp.core.help

import caseapp.core.Error
import caseapp.{ExtraName, HelpMessage, Recurse}

/**
  * Helper to add `--usage` and `--help` options to an existing type `T`.
  *
  * @param usage: whether usage was requested
  * @param help: whether help was requested
  * @param baseOrError: parsed `T` in case of success, or error message else
  * @tparam T: type to which usage and help options are added
  */
final case class WithHelp[T](
  @HelpMessage("Print usage and exit")
    usage: Boolean = false,
  @HelpMessage("Print help message and exit")
  @ExtraName("h")
    help: Boolean = false,
  @Recurse
    baseOrError: Either[Seq[Error], T]
) {
  def map[U](f: T => U): WithHelp[U] =
    copy(baseOrError = baseOrError.right.map(f))
}

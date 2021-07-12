package caseapp.core.help

import caseapp.core.Error
import caseapp.{ExtraName, Group, HelpMessage, Recurse}

final case class WithFullHelp[T](
  @Group("Help")
  @HelpMessage("Print usage and exit")
    usage: Boolean = false,
  @Group("Help")
  @HelpMessage("Print help message and exit")
  @ExtraName("h")
    help: Boolean = false,
  @Group("Help")
  @HelpMessage("Print help message, including hidden options, and exit")
  @ExtraName("fullHelp")
    helpFull: Boolean = false,
  @Recurse
    baseOrError: Either[Error, T]
) {
  def map[U](f: T => U): WithFullHelp[U] =
    copy(baseOrError = baseOrError.map(f))
}

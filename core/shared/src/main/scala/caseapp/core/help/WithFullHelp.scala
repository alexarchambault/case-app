package caseapp.core.help

import caseapp.core.Error
import caseapp.{ExtraName, Group, HelpMessage, Recurse}

final case class WithFullHelp[T](
  @Recurse
  withHelp: WithHelp[T],
  @Group("Help")
  @HelpMessage("Print help message, including hidden options, and exit")
  @ExtraName("fullHelp")
  helpFull: Boolean = false
) {
  def map[U](f: T => U): WithFullHelp[U] =
    copy(withHelp = withHelp.map(f))
}

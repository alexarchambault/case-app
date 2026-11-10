package caseapp.core.app

import caseapp.core.help.Help

/** What a command entry point needs to know about its commands, whatever their effect type
  *
  * Implemented by [[Command]], and by `IOCommand` in the cats-effect module.
  */
trait CommandLike {

  /** Names of this command, see [[Command.names]] */
  def names: List[List[String]]

  /** Group of this command, see [[Command.group]] */
  def group: String

  /** Whether this command is hidden, see [[Command.hidden]] */
  def hidden: Boolean

  /** Help message of this command, see [[CaseApp.finalHelp]] */
  def finalHelp: Help[_]
}

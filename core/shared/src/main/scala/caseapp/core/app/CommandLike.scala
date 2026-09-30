package caseapp.core.app

import caseapp.core.help.Help

/** What a command entry point needs to know about its commands, whatever their effect type
  *
  * Implemented by [[Command]], and by `IOCommand` in the cats-effect module.
  */
trait CommandLike {
  def names: List[List[String]]
  def group: String
  def hidden: Boolean
  def finalHelp: Help[_]
}

package caseapp.catseffect

import caseapp.core.app.CommandLike
import caseapp.core.help.Help
import caseapp.core.parser.Parser

abstract class IOCommand[T](implicit parser: Parser[T], help: Help[T])
    extends IOCaseApp()(parser, help) with CommandLike {
  def names: List[List[String]] =
    List(List(name))
  def group: String   = ""
  def hidden: Boolean = false
}

package caseapp.core.app

import caseapp.core.parser.Parser
import caseapp.core.help.Help

abstract class Command[T](implicit parser: Parser[T], help: Help[T]) extends CaseApp()(parser, help) {
  def names: List[List[String]] =
    List(List(name))
  def name: String =
    help.progName
  def group: String = ""
  def hidden: Boolean = false
}

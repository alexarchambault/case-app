package caseapp.core.parser

import caseapp.core.argparser.ArgParser
import caseapp.core.{Arg, Error}
import caseapp.core.util.Formatter
import caseapp.Name

trait Argument[H] {
  def arg: Arg
  def withDefaultOrigin(origin: String): Argument[H]
  def init: Option[H]
  def step(
    args: List[String],
    d: Option[H],
    nameFormatter: Formatter[Name]
  ): Either[(Error, List[String]), Option[(Option[H], List[String])]]
  def get(d: Option[H], nameFormatter: Formatter[Name]): Either[Error, H]
}

object Argument {
  def apply[H: ArgParser](arg: Arg): Argument[H] =
    StandardArgument[H](arg)
  def apply[H](
    arg: Arg,
    argParser: ArgParser[H],
    default: () => Option[H]
  ): StandardArgument[H] =
    StandardArgument[H](arg, argParser, default)
}

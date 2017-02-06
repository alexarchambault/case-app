package caseapp
package core

case class Arg(
  name: String,
  extraNames: Seq[Name],
  valueDescription: Option[ValueDescription],
  helpMessage: Option[HelpMessage],
  noHelp: Boolean,
  isFlag: Boolean,
  hintDescription: String,
  defaultDescription: Option[String]
)

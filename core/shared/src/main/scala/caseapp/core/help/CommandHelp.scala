package caseapp.core.help

import caseapp.core.Arg
import dataclass.data
import caseapp.HelpMessage

@data case class CommandHelp(
  args: Seq[Arg],
  argsNameOption: Option[String],
  helpMessage: Option[HelpMessage]
) {

  def usageMessage(progName: String, commandName: Seq[String]): String =
    s"Usage: $progName ${commandName.mkString(" ")} ${argsNameOption.map("<" + _ + ">").mkString}"

  def optionsMessage: String =
    Help.optionsMessage(args)

  def helpMessage(progName: String, commandName: Seq[String]): String = {
    val b = new StringBuilder
    b ++= s"Command: ${commandName.mkString(" ")}${Help.NL}"
    for (m <- helpMessage)
      b ++= m.message
    b ++= usageMessage(progName, commandName)
    b ++= Help.NL
    b ++= optionsMessage
    b ++= Help.NL
    b.result()
  }

}

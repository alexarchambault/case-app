package caseapp.core.help

import caseapp.core.Arg
import dataclass.data

@data class CommandHelp(
  args: Seq[Arg],
  argsNameOption: Option[String]
) {

  def usageMessage(progName: String, commandName: Seq[String]): String =
    s"Usage: $progName ${commandName.mkString(" ")} ${argsNameOption.map("<" + _ + ">").mkString}"

  def optionsMessage: String =
    Help.optionsMessage(args)

  def helpMessage(progName: String, commandName: Seq[String]): String = {
    val b = new StringBuilder
    b ++= s"Command: ${commandName.mkString(" ")}${Help.NL}"
    b ++= usageMessage(progName, commandName)
    b ++= Help.NL
    b ++= optionsMessage
    b ++= Help.NL
    b.result()
  }

}

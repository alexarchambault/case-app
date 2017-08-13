package caseapp.core.help

import caseapp.core.Arg

final case class CommandHelp(
  args: Seq[Arg],
  argsNameOption: Option[String]
) {

  def usageMessage(progName: String, commandName: String): String =
    s"Usage: $progName $commandName ${argsNameOption.map("<" + _ + ">").mkString}"

  def optionsMessage: String =
    Help.optionsMessage(args)

  def helpMessage(progName: String, commandName: String): String = {
    val b = new StringBuilder
    b ++= s"Command: $commandName${Help.NL}"
    b ++= usageMessage(progName, commandName)
    b ++= Help.NL
    b ++= optionsMessage
    b ++= Help.NL
    b.result()
  }

}

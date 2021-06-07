package caseapp.core.help

import caseapp.core.Arg
import caseapp.core.app.Command
import caseapp.core.util.fansi
import caseapp.core.util.NameOps.toNameOps
import dataclass._

@data class RuntimeCommandsHelp(
  progName: String,
  description: Option[String],
  defaultHelp: Help[_],
  commands: Seq[(List[List[String]], Help[_])]
) {

  def help(): String =
    help(HelpFormat.default())

  def help(format: HelpFormat): String = {
    val b = new StringBuilder
    b.append("Usage: ")
    b.append(format.progName(progName).render)

    if (commands.nonEmpty)
      b.append(" <COMMAND>")

    if (defaultHelp.args.nonEmpty) {
      b.append(" ")
      b.append(defaultHelp.optionsDesc)
    }

    for (argName <- defaultHelp.argsNameOption) {
      b.append(" [")
      b.append(argName)
      b.append("]")
    }

    b.append(format.newLine)

    for (desc <- description)
      Help.printDescription(b, desc, format.newLine, format.terminalWidth)

    b.append(format.newLine)

    defaultHelp.printOptions(b, format)

    if (commands.nonEmpty) {
      b.append(format.newLine)
      b.append(format.newLine)
      b.append("Commands:")
      b.append(format.newLine)
      printCommands(b, format)
    }

    b.result()
  }

  def printCommands(b: StringBuilder, format: HelpFormat): Unit =
    if (commands.nonEmpty) {
      val content = commands
        .iterator
        .map {
          case (names, commandHelp) =>
            val names0 = names.map(_.mkString(" ")).map(format.commandName(_).render).mkString(", ")
            val descOpt = commandHelp.helpMessage.flatMap(_.message.linesIterator.map(_.trim).filter(_.nonEmpty).toStream.headOption).map(x => x: fansi.Str)
            Seq(names0: fansi.Str, descOpt.getOrElse("": fansi.Str))
        }
        .toVector
      val table = Table(content)
      table.render(b, "  ", "  ", format.newLine, table.widths.map(_.min(45)).toVector)
    }

}

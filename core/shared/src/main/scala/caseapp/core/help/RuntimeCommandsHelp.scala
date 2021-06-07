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
  commands: Seq[RuntimeCommandHelp[_]]
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
      printCommands(b, format)
    }

    b.result()
  }

  def printCommands(b: StringBuilder, format: HelpFormat): Unit =
    if (commands.nonEmpty) {

      val grouped = format.sortCommandGroupValues(commands.groupBy(_.group).toVector)

      def table(commands: Seq[RuntimeCommandHelp[_]]) =
        Table {
          commands
            .iterator
            .map { help =>
              val names0 = help.names.map(_.mkString(" ")).map(format.commandName(_).render).mkString(", ")
              val descOpt = help.help.helpMessage.flatMap(_.message.linesIterator.map(_.trim).filter(_.nonEmpty).toStream.headOption).map(x => x: fansi.Str)
              Seq(names0: fansi.Str, descOpt.getOrElse("": fansi.Str))
            }
            .toVector
        }

      grouped
        .iterator
        .zipWithIndex
        .foreach {
          case ((groupName, groupCommands), idx) =>
            if (idx > 0) {
              b.append(format.newLine)
              b.append(format.newLine)
            }
            val printedName =
              if (groupName.isEmpty) {
                if (grouped.length == 1) "Commands:"
                else "Other commands:"
              } else s"$groupName commands:"
            b.append(printedName)
            b.append(format.newLine)
            val table0 = table(groupCommands)
            table0.render(b, "  ", "  ", format.newLine, table0.widths.map(_.min(45)).toVector)
        }
    }

}

package caseapp.core.app

import caseapp.core.Error
import caseapp.core.commandparser.CommandParser
import caseapp.core.help.{CommandsHelp, Help, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs

@deprecated("Use Command and CommandsEntryPoint instead", "2.1.0")
abstract class CommandAppWithPreCommand[D, T](implicit
  val beforeCommandParser: Parser[D],
  baseBeforeCommandMessages: Help[D],
  val commandParser: CommandParser[T],
  val commandsMessages: CommandsHelp[T]
) {

  def beforeCommand(options: D, remainingArgs: Seq[String]): Unit

  def run(options: T, remainingArgs: RemainingArgs): Unit

  def exit(code: Int): Nothing =
    PlatformUtil.exit(code)

  def error(message: Error): Nothing = {
    Console.err.println(message.message)
    exit(255)
  }

  lazy val beforeCommandMessages: Help[D] =
    baseBeforeCommandMessages
      .withAppName(appName)
      .withAppVersion(appVersion)
      .withProgName(progName)
      .withOptionsDesc(s"[options] [command] [command-options]")
      .asInstanceOf[Help[D]] // circumventing data-class losing the type param :|

  lazy val commands: Seq[Seq[String]] = CommandsHelp[T].messages.map(_._1)

  def helpAsked(): Nothing = {
    print(beforeCommandMessages.help)
    println(s"Available commands: ${commands.map(_.mkString(" ")).mkString(", ")}\n")
    println(s"Type  $progName command --help  for help on an individual command")
    exit(0)
  }

  def commandHelpAsked(command: Seq[String]): Nothing = {
    println(commandsMessages.messagesMap(command).helpMessage(
      beforeCommandMessages.progName,
      command
    ))
    exit(0)
  }

  def usageAsked(): Nothing = {
    println(beforeCommandMessages.usage)
    println(s"Available commands: ${commands.map(_.mkString(" ")).mkString(", ")}\n")
    println(s"Type  $progName command --usage  for usage of an individual command")
    exit(0)
  }

  def commandUsageAsked(command: Seq[String]): Nothing = {
    println(commandsMessages.messagesMap(command).usageMessage(
      beforeCommandMessages.progName,
      command
    ))
    exit(0)
  }

  def appName: String    = Help[D].appName
  def appVersion: String = Help[D].appVersion
  def progName: String   = Help[D].progName

  def main(args: Array[String]): Unit =
    commandParser.withHelp.detailedParse(PlatformUtil.arguments(args).toVector)(
      beforeCommandParser.withHelp
    ) match {
      case Left(err) =>
        error(err)

      case Right((WithHelp(usage, help, d), dArgs, optCmd)) =>
        if (help)
          helpAsked()

        if (usage)
          usageAsked()

        d.fold(
          error,
          beforeCommand(_, dArgs)
        )

        optCmd.foreach {
          case Left(err) =>
            error(err)

          case Right((c, WithHelp(commandUsage, commandHelp, t), commandArgs)) =>
            if (commandHelp)
              commandHelpAsked(c)

            if (commandUsage)
              commandUsageAsked(c)

            t.fold(
              error,
              run(_, commandArgs)
            )
        }
    }

}

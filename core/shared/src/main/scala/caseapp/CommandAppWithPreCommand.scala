package caseapp

import caseapp.core.{CommandsMessages, Messages, WithHelp}

abstract class CommandAppWithPreCommand[D, T](implicit
  val beforeCommandParser: Parser[D],
  baseBeforeCommandMessages: Messages[D],
  val commandParser: CommandParser[T],
  val commandsMessages: CommandsMessages[T]
) {

  def beforeCommand(options: D, remainingArgs: Seq[String]): Unit

  def run(options: T, remainingArgs: RemainingArgs): Unit

  def exit(code: Int): Unit =
    sys.exit(code)

  def error(message: String): Unit = {
    Console.err.println(message)
    exit(255)
  }

  lazy val beforeCommandMessages: Messages[D] = baseBeforeCommandMessages.copy(
    appName = appName,
    appVersion = appVersion,
    progName = progName,
    optionsDesc = s"[options] [command] [command-options]"
  )

  lazy val commands: Seq[String] = CommandsMessages[T].messages.map { case (c, _) => c }

  def helpAsked(): Unit = {
    print(beforeCommandMessages.helpMessage)
    println(s"Available commands: ${commands.mkString(", ")}\n")
    println(s"Type  $progName command --help  for help on an individual command")
    exit(0)
  }

  def commandHelpAsked(command: String): Unit = {
    println(commandsMessages.messagesMap(command).helpMessage(beforeCommandMessages.progName, command))
    exit(0)
  }

  def usageAsked(): Unit = {
    println(beforeCommandMessages.usageMessage)
    println(s"Available commands: ${commands.mkString(", ")}\n")
    println(s"Type  $progName command --usage  for usage of an individual command")
    exit(0)
  }

  def commandUsageAsked(command: String): Unit = {
    println(commandsMessages.messagesMap(command).usageMessage(beforeCommandMessages.progName, command))
    exit(0)
  }


  def appName: String = Messages[D].appName
  def appVersion: String = Messages[D].appVersion
  def progName: String = Messages[D].progName

  def main(args: Array[String]): Unit =
    commandParser.withHelp.detailedParse(args)(beforeCommandParser.withHelp) match {
      case Left(err) =>
        error(err)

      case Right((WithHelp(usage, help, d), dArgs, optCmd)) =>

        if (help)
          helpAsked()

        if (usage)
          usageAsked()

        d match {
          case Left(err) =>
            error(err)
          case Right(d0) =>
            beforeCommand(d0, dArgs)
        }

        optCmd.foreach {
          case Left(err) =>
            error(err)

          case Right((c, WithHelp(commandUsage, commandHelp, t), commandArgs, commandArgs0)) =>
            if (commandHelp)
              commandHelpAsked(c)

            if (commandUsage)
              commandUsageAsked(c)

            t match {
              case Left(err) =>
                error(err)
              case Right(t0) =>
                run(t0, RemainingArgs(commandArgs, commandArgs0))
            }
        }
    }

}
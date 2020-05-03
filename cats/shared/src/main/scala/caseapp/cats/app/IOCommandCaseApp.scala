package caseapp.cats.app

import caseapp.core.Error
import caseapp.core.help.{CommandsHelp, Help, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs
import cats.effect.{ExitCode, IO, IOApp}
import caseapp.core.commandparser.CommandParser

abstract class IOCommandCaseApp[T](
  implicit val beforeCommandParser: Parser[None.type],
  val commandParser: CommandParser[T],
  val commandsMessages: CommandsHelp[T]
) extends IOApp {

  protected def run(options: T, remainingArgs: RemainingArgs): IO[ExitCode]

  private def error(message: Error): IO[ExitCode] = IO {
    Console.err.println(message.message)
    ExitCode.Error
  }

  private def beforeCommandMessages: Help[None.type] = {
    Help[None.type]
      .withAppName(appName)
      .withAppVersion(appVersion)
      .withProgName(progName)
      .withOptionsDesc(s"[options] [command] [command-options]")
      .asInstanceOf[Help[None.type]]
  }

  private lazy val commands: Seq[String] = CommandsHelp[T].messages.flatMap(_._1)

  private def helpAsked(): IO[ExitCode] = IO {
    print(beforeCommandMessages.help)
    println(s"Available commands: ${commands.mkString(", ")}\n")
    println(s"Type  $progName command --help  for help on an individual command")
    ExitCode.Success
  }

  private def commandHelpAsked(command: Seq[String]): IO[ExitCode] = IO {
    println(commandsMessages.messagesMap(command).helpMessage(beforeCommandMessages.progName, command))
    ExitCode.Success
  }

  private def usageAsked(): IO[ExitCode] = IO {
    println(beforeCommandMessages.usage)
    println(s"Available commands: ${commands.mkString(", ")}\n")
    println(s"Type  $progName command --usage  for usage of an individual command")
    ExitCode.Success
  }

  private def commandUsageAsked(command: Seq[String]): IO[ExitCode] = IO {
    println(commandsMessages.messagesMap(command).usageMessage(beforeCommandMessages.progName, command))
    ExitCode.Success
  }

  val appName: String = Help[None.type].appName
  val appVersion: String = Help[None.type].appVersion
  val progName: String = Help[None.type].progName

  override def run(args: List[String]): IO[ExitCode] = {
    commandParser.withHelp.detailedParse(args.toVector)(beforeCommandParser.withHelp) match {
      case Left(err) =>
        error(err)
      case Right((WithHelp(true, _, _), _, _)) =>
        usageAsked()
      case Right((WithHelp(_, true, _), _, _)) =>
        helpAsked()
      case Right((_, _, optCmd)) =>
        optCmd
          .map {
            case Left(err) =>
              error(err)
            case Right((c, WithHelp(true, _, _), _)) =>
              commandUsageAsked(c)
            case Right((c, WithHelp(_, true, _), _)) =>
              commandHelpAsked(c)
            case Right((_, WithHelp(_, _, t), commandArgs)) =>
              t.fold(
                error,
                run(_, commandArgs)
              )
          }
          .getOrElse(IO(ExitCode.Success))
    }
  }

}

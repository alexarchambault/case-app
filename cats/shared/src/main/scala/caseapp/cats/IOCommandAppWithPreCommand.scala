package caseapp.cats

import caseapp.core.Error
import caseapp.core.commandparser.CommandParser
import caseapp.core.help.{CommandsHelp, Help, WithHelp}
import caseapp.core.parser.Parser
import caseapp.core.RemainingArgs
import cats.effect._

abstract class IOCommandAppWithPreCommand[D, T](implicit
  val beforeCommandParser: Parser[D],
  baseBeforeCommandMessages: Help[D],
  val commandParser: CommandParser[T],
  val commandsMessages: CommandsHelp[T]
) extends IOApp {

  /**
    * Override to support conditional early exit, suppressing a run.
    * @param options parsed options
    * @param remainingArgs extra arguments
    * @return exit code for early exit, none to call run
    */
  def beforeCommand(options: D, remainingArgs: Seq[String]): IO[Option[ExitCode]]

  def run(options: T, remainingArgs: RemainingArgs): IO[ExitCode]

  def error(message: Error): IO[ExitCode] = IO {
    Console.err.println(message.message)
    ExitCode(255)
  }

  lazy val beforeCommandMessages: Help[D] =
    baseBeforeCommandMessages
      .withAppName(appName)
      .withAppVersion(appVersion)
      .withProgName(progName)
      .withOptionsDesc(s"[options] [command] [command-options]")
      .asInstanceOf[Help[D]] // circumventing data-class losing the type param :|

  lazy val commands: Seq[Seq[String]] = CommandsHelp[T].messages.map(_._1)

  def helpAsked(): IO[ExitCode] = IO {
    print(beforeCommandMessages.help)
    println(s"Available commands: ${commands.map(_.mkString(" ")).mkString(", ")}\n")
    println(s"Type  $progName command --help  for help on an individual command")
    ExitCode.Success
  }

  def commandHelpAsked(command: Seq[String]): IO[ExitCode] = IO {
    println(commandsMessages.messagesMap(command).helpMessage(beforeCommandMessages.progName, command))
    ExitCode.Success
  }

  def usageAsked(): IO[ExitCode] = IO {
    println(beforeCommandMessages.usage)
    println(s"Available commands: ${commands.map(_.mkString(" ")).mkString(", ")}\n")
    println(s"Type  $progName command --usage  for usage of an individual command")
    ExitCode.Success
  }

  def commandUsageAsked(command: Seq[String]): IO[ExitCode] = IO {
    println(commandsMessages.messagesMap(command).usageMessage(beforeCommandMessages.progName, command))
    ExitCode.Success
  }


  def appName: String = Help[D].appName
  def appVersion: String = Help[D].appVersion
  def progName: String = Help[D].progName

  override def run(args: List[String]): IO[ExitCode] = {
    commandParser.withHelp.detailedParse(args.toVector)(beforeCommandParser.withHelp) match {
      case Left(err) =>
        error(err)
      case Right((WithHelp(true, _, _), _, _)) =>
        usageAsked()
      case Right((WithHelp(_, true, _), _, _)) =>
        helpAsked()
      case Right((WithHelp(false, false, Left(err)), _, _)) =>
        error(err)
      case Right((WithHelp(false, false, Right(d)), dArgs, optCmd)) =>
        beforeCommand(d, dArgs).flatMap {
          case Some(exitCode) => IO.pure(exitCode)
          case None =>
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

}

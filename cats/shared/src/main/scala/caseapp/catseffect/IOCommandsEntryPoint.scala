package caseapp.catseffect

import caseapp.core.app.CommandsEntryPointLike
import cats.effect.{ExitCode, IO, IOApp}

abstract class IOCommandsEntryPoint extends IOApp with CommandsEntryPointLike[IOCommand[_]] {

  def printUsage(): IO[ExitCode] = {
    val usage = help.help(helpFormat, showHidden = false)
    IO(Console.println(usage)).as(ExitCode.Success)
  }

  override def run(args: List[String]): IO[ExitCode] =
    commandFor(args) match {
      case None =>
        printUsage()
      case Some((commandProgName, command, commandArgs)) =>
        command.main(commandProgName, commandArgs.toArray)
    }
}

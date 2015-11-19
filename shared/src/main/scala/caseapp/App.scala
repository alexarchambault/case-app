package caseapp

import caseapp.core._
import caseapp.core.util._

import derive.AnnotationOption
import shapeless.Typeable

import scala.collection.mutable.ListBuffer


trait DefaultArgsApp extends ArgsApp {
  private var remainingArgs0 = Seq.empty[String]

  def setRemainingArgs(remainingArgs: Seq[String]): Unit =
    remainingArgs0 = remainingArgs

  def remainingArgs: Seq[String] = remainingArgs0
}

trait DefaultCommandArgsApp extends DefaultArgsApp with CommandArgsApp {
  private var command0 = Option.empty[Either[String, String]]

  def setCommand(cmd: Option[Either[String, String]]): Unit = {
    command0 = cmd
  }

  def command: Option[Either[String, String]] = command0
}

/*
 * DelayedInit à la scala.App
 * Using it in spite of https://github.com/scala/scala/pull/3563
 */
trait App extends DefaultArgsApp with DelayedInit {
  private val initCode = new ListBuffer[() => Unit]

  override def delayedInit(body: => Unit): Unit =
    initCode += (() => body)

  def apply(): Unit = {
    for (proc <- initCode)
      proc()
  }
}

// FIXME Could the content of that be de-duplicated with App above?
// Wouldn't this break delayed initialization?
trait Command extends DefaultCommandArgsApp with DelayedInit {
  private val initCode = new ListBuffer[() => Unit]

  override def delayedInit(body: => Unit): Unit =
    initCode += (() => body)

  def apply(): Unit = {
    for (proc <- initCode)
      proc()
  }
}

/**
 * Have a singleton extends this class to get a class with a main method for the app of `T`
 */
abstract class AppOf[T <: ArgsApp : Parser : Messages] {
  def main(args: Array[String]): Unit =
    CaseApp.parseWithHelp[T](args) match {
      case Left(err) =>
        Console.err.println(err)
        sys.exit(1)

      case Right((t, help, usage, remainingArgs)) =>
        if (help) {
          CaseApp.printHelp[T]()
          sys.exit(0)
        }

        if (usage) {
          CaseApp.printUsage[T]()
          sys.exit(0)
        }

        t.setRemainingArgs(remainingArgs)
        t()
    }
}

abstract class CommandAppOfWithBase[D <: CommandArgsApp : Parser : Messages, T <: ArgsApp : CommandParser : CommandsMessages] {
  def appName: String = Messages[D].appName
  def appVersion: String = Messages[D].appVersion
  def progName: String = Messages[D].progName

  def main(args: Array[String]): Unit = {
    val messages = Messages[D].copy(
      appName = appName,
      appVersion = appVersion,
      progName = progName,
      optionsDesc = s"[options] [command] [command-options]"
    )

    CommandParser[T].withHelp.apply(args)(Parser[D].withHelp) match {
      case Left(err) =>
        Console.err.println(err)
        sys.exit(255)

      case Right((WithHelp(usage, help, d), dArgs, optCmd)) =>
        val commands = CommandsMessages[T].messages.map { case (c, _) => c }

        if (help) {
          print(messages.helpMessage)
          println(s"Available commands: ${commands.mkString(", ")}\n")
          println(s"Type  $progName command --help  for help on an individual command")
          sys.exit(0)
        }

        if (usage) {
          println(messages.usageMessage)
          println(s"Available commands: ${commands.mkString(", ")}\n")
          println(s"Type  $progName command --usage  for usage of an individual command")
          sys.exit(0)
        }

        d.setRemainingArgs(dArgs)
        d.setCommand(optCmd.map(_.right.map { case (c, _, _) => c }))
        d()

        optCmd.foreach {
          case Left(err) =>
            Console.err.println(err)
            sys.exit(255)
          case Right((c, WithHelp(usage, help, t), args)) =>
            if (help) {
              println(CommandsMessages[T].map(c).helpMessage(messages.progName, c))
              sys.exit(0)
            }

            if (usage) {
              println(CommandsMessages[T].map(c).usageMessage(messages.progName, c))
              sys.exit(0)
            }

            t.setRemainingArgs(args)
            t()
        }
    }
  }
}

case class DefaultBaseCommand() extends Command {
  override def setCommand(cmd: Option[Either[String, String]]): Unit = {
    if (cmd.isEmpty) {
      // FIXME Print available commands too?
      Console.err.println("Error: no command specified")
      sys.exit(255)
    }
    super.setCommand(cmd)
  }
}

// FIXME Not sure Typeable is fine on Scala JS, should be replaced by something else
abstract class CommandAppOf[T <: ArgsApp](implicit
  commandParser: CommandParser[T],
  commandMessages: CommandsMessages[T],
  typeable: Typeable[T],
  appName0: AnnotationOption[AppName, T],
  appVersion0: AnnotationOption[AppVersion, T],
  progName0: AnnotationOption[ProgName, T]
) extends CommandAppOfWithBase[DefaultBaseCommand, T] {

  lazy val tpeDesc = typeable.describe

  override def appName: String =
    appName0().fold(tpeDesc)(_.appName)
  override def appVersion: String =
    appVersion0().fold("")(_.appVersion)
  override def progName: String =
    progName0().fold(pascalCaseSplit(tpeDesc.toList).map(_.toLowerCase).mkString("-"))(_.progName)

}
package caseapp

import caseapp.core._
import caseapp.core.util._

import caseapp.util.AnnotationOption
import shapeless.Typeable

import scala.collection.mutable.ListBuffer


/**
 * Have a case class extend this trait for its fields to become command line arguments,
 * and its body the core of your app using these.
 *
 * Extends `DelayedInit`, so that the body of the case class gets called later.
 *
 * Remaining arguments are accessible via the method `remainingArgs`.
 *
 * Example
 * {{{
 *   case class Foo(
 *     i: Int,
 *     foo: String
 *   ) extends App {
 *
 *     // core of your app, using the fields above
 *
 *   }
 *
 *   object FooApp extends AppOf[Foo]
 * }}}
 *
 * In the example above, `FooApp` now has a `main` method, that parses the arguments it is given,
 * and matches these to the fields `i` (`-i 2` gives `i` the value `2`) and `foo` (`--foo ab`
 * gives `foo` the value `"ab"`) of `Foo`. It also accepts `--help` / `-h` / `--usage` arguments,
 * and prints help or usage messages when these are present.
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

/**
 * Have a sealed trait extend this for its case class children to become commands.
 *
 * Extends `DelayedInit` like `App` does.
 *
 * Like with `App`, the remaining arguments are accessible with the method `remainingArgs`.
 *
 * Example
 * {{{
 *   sealed trait DemoCommand extends Command
 *
 *   case class First(
 *   ) extends DemoCommand {
 *
 *     // ...
 *
 *   }
 *
 *   case class Second(
 *   ) extends DemoCommand {
 *
 *     // ...
 *
 *   }
 *
 *   object MyApp extends CommandAppOf[DemoCommand]
 * }}}
 *
 * In the example above, `MyApp` now has a `main` method, that accepts arguments
 * like `first a b` or `second c d`. In the first case, it will create a `First`, and
 * call its body (whose initialization is delayed thanks to delayed initialization). In the
 * second case, it will create a `Second` instead, and call its body too.
 */
trait Command extends DefaultCommandArgsApp with DelayedInit {
  // FIXME Could the content of that be de-duplicated with App above?
  // Wouldn't this break delayed initialization?

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
    Parser[T].withHelp.detailedParse(args) match {
      case Left(err) =>
        Console.err.println(err)
        sys.exit(1)

      case Right((WithHelp(usage, help, t), remainingArgs, extraArgs)) =>
        if (help) {
          println(Messages[T].withHelp.helpMessage)
          sys.exit(0)
        }

        if (usage) {
          println(Messages[T].withHelp.usageMessage)
          sys.exit(0)
        }

        t.setRemainingArgs(remainingArgs, extraArgs)
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

    CommandParser[T].withHelp.detailedParse(args)(Parser[D].withHelp) match {
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

        d.setRemainingArgs(dArgs, Nil)
        d.setCommand(optCmd.map(_.right.map { case (c, _, _, _) => c }))
        d()

        optCmd.foreach {
          case Left(err) =>
            Console.err.println(err)
            sys.exit(255)
          case Right((c, WithHelp(usage, help, t), args, args0)) =>
            if (help) {
              println(CommandsMessages[T].messagesMap(c).helpMessage(messages.progName, c))
              sys.exit(0)
            }

            if (usage) {
              println(CommandsMessages[T].messagesMap(c).usageMessage(messages.progName, c))
              sys.exit(0)
            }

            t.setRemainingArgs(args, args0)
            t()
        }
    }
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

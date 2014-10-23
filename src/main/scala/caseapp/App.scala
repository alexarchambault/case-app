package caseapp

import language.reflectiveCalls
import scala.reflect.runtime.universe.{ Try => _, _ }
import scala.util.{ Success, Failure }
import scala.collection.mutable.ListBuffer


trait ArgsApp {
  private[caseapp] def setRemainingArgs(remainingArgs: Seq[String]): Unit
  def remainingArgs: Seq[String]
  def apply(): Unit 
}

/*
 * DelayedInit à la scala.App
 * Using it in spite of https://github.com/scala/scala/pull/3563
 */
trait App extends ArgsApp with DelayedInit {
  private val initCode = new ListBuffer[() => Unit]
  private var _remainingArgs = Seq.empty[String]

  private[caseapp] def setRemainingArgs(remainingArgs: Seq[String]): Unit = {
    _remainingArgs = remainingArgs
  }

  def remainingArgs: Seq[String] = _remainingArgs

  override def delayedInit(body: => Unit): Unit = {
    initCode += (() => body)
  }

  def apply(): Unit = {
    for (proc <- initCode)
      proc()
  }
}

abstract class AppOf[C <: ArgsApp] {

  /*
   * See https://issues.scala-lang.org/browse/SI-5000
   * and https://issues.scala-lang.org/browse/SI-7666
   */
  final def me(implicit parser: Parser[WithHelp[C]], messages: Messages[C]): (Parser[WithHelp[C]], Messages[C]) =
    (parser, messages)
  def ignore: (Parser[WithHelp[C]], Messages[C])


  private lazy val (_parser, _messages) = ignore

  def main(args: Array[String]): Unit =
    _parser(args.toList) match {
      case Success((a, remaining)) =>
        a.setRemainingArgs(remaining)
        a()

      case Failure(t: IllegalArgumentException) =>
        Console.err.println(s"Illegal argument: ${t.getMessage}")
        Console.err.println(_messages.usageMessage)
        sys exit 1

      case Failure(t) =>
        Console.err.println(t)
        sys exit 1
    }
}

package caseapp

import language.reflectiveCalls
import reflect.runtime.universe.{ Try => _, _ }
import scala.util.{ Success, Failure }
import collection.mutable.ListBuffer
import core.{ Messages, ArgsApp }


/*
 * DelayedInit à la scala.App
 * Using it in spite of https://github.com/scala/scala/pull/3563
 */
trait App extends ArgsApp with DelayedInit {
  private val initCode = new ListBuffer[() => Unit]
  private var _remainingArgs = Seq.empty[String]

  def setRemainingArgs(remainingArgs: Seq[String]): Unit =
    _remainingArgs = remainingArgs

  def remainingArgs: Seq[String] = _remainingArgs

  override def delayedInit(body: => Unit): Unit =
    initCode += (() => body)

  def apply(): Unit = {
    for (proc <- initCode)
      proc()
  }
}

/**
 * Have a singleton extends this class to get a class with a main method for the app of `C`
 */
abstract class AppOf[C <: ArgsApp] {
  /*
   * See https://issues.scala-lang.org/browse/SI-5000
   * and https://issues.scala-lang.org/browse/SI-7666
   */
  final def default(implicit parser: Parser[WithHelp[C]], messages: Messages[C]): (Parser[WithHelp[C]], Messages[C]) =
    (parser, messages)
  def parser: (Parser[WithHelp[C]], Messages[C])


  private lazy val (_parser, _messages) = parser

  def main(args: Array[String]): Unit =
    _parser(args.toList) match {
      case Success((a, remaining)) =>
        a setRemainingArgs remaining
        a()

      case Failure(t: IllegalArgumentException) =>
        Console.err println s"Illegal argument: ${t.getMessage}"
        Console.err println _messages.usageMessage
        sys exit 1

      case Failure(t) =>
        Console.err println t
        sys exit 1
    }
}

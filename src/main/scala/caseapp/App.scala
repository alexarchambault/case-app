package caseapp

import collection.mutable.ListBuffer
import caseapp.core._


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
 * Have a singleton extends this class to get a class with a main method for the app of `T`
 */
abstract class AppOf[T <: ArgsApp] {
  /*
   * Getting these type classes this way and not with AppOf[C <: ArgsApp : Default : NamesOf : ...]
   * because of https://issues.scala-lang.org/browse/SI-5000 and https://issues.scala-lang.org/browse/SI-7666
   */
  final def default(implicit default: Default[T], namesOf: NamesOf[T], argParser: ArgParser[T], messages: Messages[T]): (Default[T], NamesOf[T], ArgParser[T], Messages[T]) =
    (default, namesOf, argParser, messages)
  def parser: (Default[T], NamesOf[T], ArgParser[T], Messages[T])


  private implicit lazy val (_default, _namesOf, _argParser, _messages) = parser

  def main(args: Array[String]): Unit =
    CaseApp.parseWithHelp[T](args) match {
      case Left(err) =>
        Console.err println err
        sys exit 1

      case Right((t, help, usage, remainingArgs)) =>
        if (help) {
          CaseApp.printHelp[T]()
          sys exit 0
        }

        if (usage) {
          CaseApp.printUsage[T]()
          sys exit 0
        }

        t setRemainingArgs remainingArgs
        t()
    }
}

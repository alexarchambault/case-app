package caseapp

import caseapp.core._
import shapeless.{ LabelledGeneric, Lazy, Witness }

import scala.util.{ Success, Failure }

object CaseApp {

  def parse[T: Default : NamesOf : ArgParser](args: Seq[String]): Either[String, (T, Seq[String])] = {
    val _parser = ArgParser[T].apply((Some(NamesOf[T].apply()), Nil))

    def helper(current: T, args: List[String], extraArgsReverse: List[String]): Either[String, (T, List[String])] =
      args match {
        case Nil =>
          Right((current, extraArgsReverse.reverse))
        case args =>
          _parser(current, args) match {
            case Success(None) =>
              args match {
                case "--" :: t =>
                  helper(current, Nil, (extraArgsReverse /: t)(_.::(_)))
                case opt :: _ if opt startsWith "-" =>
                  Left(s"Unrecognized argument: $opt")
                case userArg :: rem =>
                  helper(current, rem, userArg :: extraArgsReverse)
              }

            case Success(Some((newC, newArgs))) =>
              assert(newArgs != args)
              helper(newC, newArgs, extraArgsReverse)

            case Failure(t) =>
              Left(t.getMessage)
          }
      }

    helper(Default[T].apply(), args.toList, Nil)
  }

  private case class WithHelp[T](
    usage: Boolean = false,
    @ExtraName("h") help: Boolean = false,
    base: T
  )

  def parseWithHelp[T: Default : NamesOf : ArgParser](args: Seq[String]): Either[String, (T, Boolean, Boolean, Seq[String])] = {
    // Defining explicitly the type classes of WithHelp[T] because of https://github.com/milessabin/shapeless/issues/314
    // See also the discussion about it on https://gitter.im/milessabin/shapeless around the same time

    implicit val withHelpDefault =
      Default.from[WithHelp[T]] {
        WithHelp(
          usage = false,
          help = false,
          Default[T].apply()
        )
      }

    implicit val namesOfWithHelp =
      NamesOf.from[WithHelp[T]](
        Names(List(
          "usage" -> Right(List(Name("usage"))),
          "help" -> Right(List(Name("help"), Name("h"))),
          "base" -> Left(NamesOf[T].apply())
        ))
      )

    implicit val withHelpArgParser =
      ArgParser.instanceArgParser(
        LabelledGeneric[WithHelp[T]],
        Lazy(ArgParser.hconsArgParser(
          Witness('usage),
          Lazy(implicitly[ArgParser[Boolean]]),
          ArgParser.hconsArgParser(
            Witness('help),
            Lazy(implicitly[ArgParser[Boolean]]),
            ArgParser.hconsArgParser(
              Witness('base),
              Lazy(implicitly[ArgParser[T]]),
              ArgParser.hnilArgParser
            )
          )
        ))
      )

    parse[WithHelp[T]](args).right map {
      case (WithHelp(usage, help, base), rem) =>
        (base, help, usage, rem)
    }
  }

  def helpMessage[T: Messages]: String =
    Messages[T].helpMessage

  def usageMessage[T: Messages]: String =
    Messages[T].usageMessage

  def printHelp[T: Messages](err: Boolean = false): Unit =
    (if (err) Console.err else Console.out) println helpMessage[T]

  def printUsage[T: Messages](err: Boolean = false): Unit =
    (if (err) Console.err else Console.out) println usageMessage[T]

}

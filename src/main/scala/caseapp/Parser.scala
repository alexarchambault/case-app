package caseapp

import core.{NamesOf, ArgParser, Default}
import scala.util.{ Try, Success, Failure }

/**
 * Argument parser for `C`
 */
trait Parser[C] {
  /**
   * Given arguments `args`, returns either:
   *  - a `Failure` if wrong arguments were supplied,
   *  - or a `Success((c, remainingArgs))` else, with `c` an instance of `C`, and `remainingArgs` the
   *    remaining arguments.
   */
  def apply(args: List[String]): Try[(C, List[String])]
}

object Parser {
  def apply[C](implicit parser: Parser[C]): Parser[C] = parser

  def from[C](f: List[String] => Try[(C, List[String])]): Parser[C] = new Parser[C] {
    def apply(args: List[String]) = f(args)
  }

  implicit def parser[C : Default : NamesOf : ArgParser]: Parser[C] = {
    val _parser = ArgParser[C].apply(Left(NamesOf[C].apply()))

    def helper(current: C, args: List[String], extraArgsReverse: List[String]): Try[(C, List[String])] =
      args match {
        case Nil =>
          Success((current, extraArgsReverse.reverse))
        case args =>
          _parser(current, args) match {
            case Success(None) =>
              args match {
                case "--" :: t =>
                  helper(current, Nil, (extraArgsReverse /: t)(_.::(_)))
                case opt :: _ if opt startsWith "-" =>
                  Failure(new IllegalArgumentException(opt))
                case userArg :: rem =>
                  helper(current, rem, userArg :: extraArgsReverse)
              }

            case Success(Some((newC, newArgs))) =>
              assert(newArgs != args)
              helper(newC, newArgs, extraArgsReverse)

            case Failure(t) =>
              Failure(t)
          }
      }

    Parser.from(helper(Default[C].apply(), _, Nil))
  }
}

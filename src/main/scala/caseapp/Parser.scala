package caseapp

import caseapp.internals.{RecNamesOf, PreFolder, Default}
import scala.util.{ Try, Success, Failure }

trait Parser[C] {
  def apply(args: List[String]): Try[(C, List[String])]
}

object Parser {

  def apply[C](f: List[String] => Try[(C, List[String])]): Parser[C] = new Parser[C] {
    def apply(args: List[String]) = f(args)
  }

  implicit def parser[C : Default : RecNamesOf : PreFolder]: Parser[C] = {
    val folder = implicitly[PreFolder[C]].apply(Left(implicitly[RecNamesOf[C]].apply()))

    def helper(current: C, args: List[String], extraArgsReverse: List[String]): Try[(C, List[String])] =
      args match {
        case Nil =>
          Success((current, extraArgsReverse.reverse))
        case args =>
          folder(current, args) match {
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

    Parser(helper(implicitly[Default[C]].apply(), _, Nil))
  }

}

package caseapp

import caseapp.core.ArgParser

object Definitions {

  case class NoArgs() extends App

  case class FewArgs(
    value  : String = "default",
    numFoo : Int = -10
  ) extends App

  case class MoreArgs(
    count  : Int @@ Counter,
    @Recurse few    : FewArgs
  ) extends App

  case class WithList(
    list   : List[Int]
  ) extends App

  case class WithTaggedList(
    list   : List[String]
  ) extends App

  case class OptBool(
    opt    : Option[Boolean]
  ) extends App

  case class Custom(s: String)

  implicit val customArgParser: ArgParser[Custom] = ArgParser.instance[Custom] { arg =>
    Right(Custom(arg))
  }

  case class WithCustom(
    custom   : Custom = Custom("")
  ) extends App

  case class Demo(
    first: Boolean = false,
    @ExtraName("V") value: Option[String] = None,
    @ExtraName("v") verbose: Int @@ Counter,
    @ExtraName("S") stages: List[String]
  ) extends App


  Parser[NoArgs]
  Parser[FewArgs]
  Parser[MoreArgs]
  Parser[WithList]
  Parser[WithTaggedList]
  Parser[OptBool]
  Parser[WithCustom]
  Parser[Demo]

  case class ReadmeOptions1(
    user: Option[String],
    enableFoo: Boolean,
    @ExtraName("f") file: List[String]
  )
  case class AuthOptions(
    user: String,
    password: String
  )

  case class PathOptions(
    @ExtraName("f") fooPath: String,
    @ExtraName("b") barPath: String
  )

  case class ReadmeOptions2(
    @Recurse auth: AuthOptions,
    @Recurse paths: PathOptions
  )

}
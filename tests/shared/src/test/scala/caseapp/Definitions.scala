package caseapp

import caseapp.core.ArgParser

object Definitions {

  final case class NoArgs()

  final case class FewArgs(
    value  : String = "default",
    numFoo : Int = -10
  )

  final case class MoreArgs(
    count  : Int @@ Counter,
    @Recurse few    : FewArgs
  )

  final case class WithList(
    list   : List[Int]
  )

  final case class WithTaggedList(
    list   : List[String]
  )

  final case class OptBool(
    opt    : Option[Boolean]
  )

  final case class Custom(s: String)

  implicit val customArgParser: ArgParser[Custom] = {
    ArgParser.instance("custom parameter") { arg =>
      Right(Custom(arg))
    }
  }

  final case class WithCustom(
    custom   : Custom = Custom("")
  )

  final case class Demo(
    first: Boolean = false,
    @ExtraName("V") value: Option[String] = None,
    @ExtraName("v") verbose: Int @@ Counter,
    @ExtraName("S") stages: List[String]
  )


  Parser[NoArgs]
  Parser[FewArgs]
  Parser[MoreArgs]
  Parser[WithList]
  Parser[WithTaggedList]
  Parser[OptBool]
  Parser[WithCustom]
  Parser[Demo]

  final case class ReadmeOptions1(
    user: Option[String],
    enableFoo: Boolean = false,
    @ExtraName("f") file: List[String]
  )
  final case class AuthOptions(
    user: String,
    password: String
  )

  final case class PathOptions(
    @ExtraName("f") fooPath: String = "",
    @ExtraName("b") barPath: String = ""
  )

  final case class ReadmeOptions2(
    @Recurse auth: AuthOptions,
    @Recurse paths: PathOptions
  )

  final case class ReadmeOptions3(
    @Recurse auth: Option[AuthOptions],
    @Recurse paths: PathOptions
  )

  final case class Example(
    foo: String,
    bar: Int
  )
}
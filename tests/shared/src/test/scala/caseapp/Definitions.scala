package caseapp

import caseapp.core.parser.Parser
import caseapp.core.{Error, Indexed}
import caseapp.core.argparser.{ArgParser, SimpleArgParser}

object Definitions {

  final case class NoArgs()

  final case class FewArgs(
    value: String = "default",
    numFoo: Int = -10
  )

  object FewArgs {
    implicit val parser: Parser[FewArgs] = Parser.derive
  }

  final case class FewArgs1(
    value: String = "default",
    numFoo: Last[Int] = Last(-10)
  )

  final case class MoreArgs(
    count: Int @@ Counter,
    @Recurse few: FewArgs
  )

  final case class WithList(
    list: List[Int]
  )

  final case class WithTaggedList(
    list: List[String]
  )

  final case class OptBool(
    opt: Option[Boolean]
  )

  final case class Custom(s: String)

  implicit val customArgParser: ArgParser[Custom] =
    SimpleArgParser.from("custom parameter") { arg =>
      Right(Custom(arg))
    }

  @AppName("WithCustom")
  final case class WithCustom(
    custom: Custom = Custom("")
  )

  final case class Demo(
    first: Boolean = false,
    @ExtraName("V") value: Option[String] = None,
    @ExtraName("v") verbose: Int @@ Counter,
    @ExtraName("S") stages: List[String]
  )

  final case class OverriddenParser(n: Int)

  object OverriddenParser {
    implicit val parser: Parser[OverriddenParser] =
      Parser.nil
        .add[Int]("count")
        .as[OverriddenParser]
  }

  val noArgsParser: Parser[NoArgs]                 = Parser.derive
  val fewArgsParser: Parser[FewArgs]               = Parser.derive
  val moreArgsParser: Parser[MoreArgs]             = Parser.derive
  val withListParser: Parser[WithList]             = Parser.derive
  val withTaggedListParser: Parser[WithTaggedList] = Parser.derive
  val optBoolParser: Parser[OptBool]               = Parser.derive
  val withCustomParser: Parser[WithCustom]         = Parser.derive
  val demoParser: Parser[Demo]                     = Parser.derive

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

  final case class ReadmeOptions4(
    @Recurse auth: Either[Error, AuthOptions],
    @Recurse paths: PathOptions
  )

  final case class ReadmeOptions5(
    fooBar: Double
  )

  final case class Example(
    foo: String,
    bar: Int
  )

  @HelpMessage("Example help message", "", "Example detailed help message")
  final case class ExampleWithHelpMessage(
    foo: String,
    bar: Int
  )

  @HelpMessage("Example help message")
  final case class SimpleExampleWithHelpMessage(
    foo: String,
    bar: Int
  )

  sealed trait Command

  case class First(
    @ExtraName("f")
    foo: String = "",
    bar: Int = 0
  ) extends Command

  case class Second(
    fooh: String = "",
    baz: Int = 0
  ) extends Command

  @HelpMessage("Third help message")
  case class Third(
    third: Int = 0
  ) extends Command

  case class Default0(bah: Double = 0.0)

  object Duplicates {

    case class Foo(fooBar: String, `foo-bar`: Int)

    case class Bar(fooBar: String, @Name("foo-bar") other: Int)

    case class First(fooBar: String)
    case class Second(@Recurse first: First, `foo-bar`: Int)

  }

  case class DefaultsThrow(
    n: Int = throw new DefaultsThrow.DefaultCalled("n"),
    s: String = throw new DefaultsThrow.DefaultCalled("s")
  )

  object DefaultsThrow {
    final class DefaultCalled(varName: String) extends Exception(s"$varName default called")
  }

  final case class FewArgsWithIndexed(
    value: Indexed[String] = Indexed("default"),
    numFoo: Indexed[Int] = Indexed(-10)
  )

  final case class WithIndexed(
    @Recurse few: FewArgsWithIndexed = FewArgsWithIndexed(),
    aa: Option[Indexed[Boolean]] = None,
    elem: List[Indexed[String]] = Nil
  )

}

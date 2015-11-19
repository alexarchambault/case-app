package caseapp

import java.util.GregorianCalendar
import caseapp.core.{ CommandParser, ArgParser }
import org.scalatest._

object Tests {

  case class NoArgs() extends App
  
  case class FewArgs(
    value  : String = "default"
  , numFoo : Int = -10
  ) extends App
  
  case class MoreArgs(
    count  : Int @@ Counter
  , @Recurse few    : FewArgs
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

  case class WithCalendar(
    date   : java.util.Calendar
  ) extends App

  case class Custom(s: String)

  implicit val customArgParser: ArgParser[Custom] = ArgParser.instance[Custom] { arg =>
    Right(Custom(arg))
  }

  case class WithCustom(
    custom   : Custom = Custom("")
  ) extends App

  case class Demo(
    first: Boolean = false
  , @ExtraName("V") value: Option[String] = None
  , @ExtraName("v") verbose: Int @@ Counter
  , @ExtraName("S") stages: List[String]
  ) extends App


  CaseApp.parseWithHelp[NoArgs] _
  CaseApp.parseWithHelp[FewArgs] _
  CaseApp.parseWithHelp[MoreArgs] _
  CaseApp.parseWithHelp[WithList] _
  CaseApp.parseWithHelp[WithTaggedList] _
  CaseApp.parseWithHelp[OptBool] _
  CaseApp.parseWithHelp[WithCalendar] _
  CaseApp.parseWithHelp[WithCustom] _
  CaseApp.parseWithHelp[Demo] _

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

class Tests extends FlatSpec with Matchers {
  
  import Tests._

  "A parser" should "parse no args" in {
    CaseApp.parse[NoArgs](Seq.empty) shouldEqual Right((NoArgs(), Seq.empty))
  }

  it should "find an illegal argument" in {
    CaseApp.parse[NoArgs](Seq("-a")).isLeft shouldBe true
  }

  it should "handle extra user arguments" in {
    CaseApp.parse[NoArgs](Seq("--", "b", "-a", "--other")) shouldEqual Right((NoArgs(), Seq("b", "-a", "--other")))
  }

  it should "give remaining args as is" in {
    CaseApp.parse[NoArgs](Seq("user arg", "other user arg")) shouldEqual Right((NoArgs(), Seq("user arg", "other user arg")))
  }

  it should "parse no args and return default values and remaining args" in {
    CaseApp.parse[FewArgs](Seq("user arg", "other user arg")) shouldEqual Right((FewArgs(), Seq("user arg", "other user arg")))
  }

  it should "parse a few args and return a default value and remaining args" in {
    CaseApp.parse[FewArgs](Seq("user arg", "--num-foo", "4", "other user arg")) shouldEqual Right((FewArgs(numFoo = 4), Seq("user arg", "other user arg")))
  }

  it should "parse a args recursively and return a default value and remaining args" in {
    CaseApp.parse[MoreArgs](Seq("user arg", "--num-foo", "4", "--count", "other user arg", "--count")) shouldEqual Right((MoreArgs(count = Tag of 2, few = FewArgs(numFoo = 4)), Seq("user arg", "other user arg")))
  }
  
  it should "parse args" in {
    CaseApp.parse[demo.Demo](Seq("user arg", "--stages", "first", "--value", "Some value", "--verbose", "--verbose", "--verbose", "other user arg", "--stages", "second", "--first")) shouldEqual Right((demo.Demo(first = true, value = Some("Some value"), verbose = Tag of 3, stages = List("first", "second")), Seq("user arg", "other user arg")))
  }

  it should "parse short args" in {
    CaseApp.parse[demo.Demo](Seq("user arg", "-S", "first", "--value", "Some value", "-v", "-v", "-v", "other user arg", "-S", "second", "--first")) shouldEqual Right((demo.Demo(first = true, value = Some("Some value"), verbose = Tag of 3, stages = List("first", "second")), Seq("user arg", "other user arg")))
  }

  it should "parse list args" in {
    CaseApp.parse[WithList](Seq("--list", "2", "--list", "5", "extra")) shouldEqual Right((WithList(list = List(2, 5)), Seq("extra")))
  }

  it should "parse semi-colon separated list args" in {
    CaseApp.parse[WithTaggedList](Seq("--list", "foo", "--list", "bar", "--list", "other", "extra2")) shouldEqual Right((WithTaggedList(list = List("foo", "bar", "other")), Seq("extra2")))
  }

  it should "parse a date" in {
    CaseApp.parse[WithCalendar](Seq("--date", "2014-10-23")) shouldEqual Right((WithCalendar(date = {
      new GregorianCalendar(2014, 9, 23)
    }), Seq.empty))
  }

  it should "parse a user-defined argument type" in {
    CaseApp.parse[WithCustom](Seq("--custom", "a")) shouldEqual Right((WithCustom(custom = Custom("a")), Seq.empty))
  }

  it should "parse first README options" in {
    CaseApp.parse[ReadmeOptions1](Seq("--user", "aaa", "--enable-foo", "--file", "some_file", "extra_arg", "other_extra_arg")) shouldEqual Right((
      ReadmeOptions1(Some("aaa"), enableFoo = true, List("some_file")),
      Seq("extra_arg", "other_extra_arg")
    ))
  }

  it should "parse first README options (second args example)" in {
    CaseApp.parse[ReadmeOptions1](Seq("--user", "bbb", "-f", "first_file", "-f", "second_file")) shouldEqual Right((
      ReadmeOptions1(Some("bbb"), enableFoo = false, List("first_file", "second_file")),
      Seq()
    ))
  }

  it should "parse second README options" in {
    CaseApp.parse[ReadmeOptions2](Seq("--user", "aaa", "--password", "pass", "extra", "-b", "bar")) shouldEqual Right((
      ReadmeOptions2(AuthOptions("aaa", "pass"), PathOptions("", "bar")),
      Seq("extra")
    ))
  }

  it should "parse commands" in {
    sealed trait Command

    case class First(
      @ExtraName("f") foo: String,
      bar: Int
    ) extends Command

    case class Second(
      fooh: String,
      baz: Int
    ) extends Command

    case class Default0(bah: Double) // = 1.0, wait for fix from shapeless

    val parser = CommandParser[Command]

    parser[Default0](Nil) shouldBe Right((Default0(0.0), Nil, None))
    parser[Default0](Seq("--wrong")) shouldBe Left(s"Unrecognized argument: --wrong")
    parser[Default0](Seq("--bah", "2")) shouldBe Right((Default0(2.0), Nil, None))
    parser[Default0](Seq("--bah", "2", "--", "other", "otherother")) shouldBe Right((Default0(2.0), Seq("other", "otherother"), None))
    parser[Default0](Seq("first")) shouldBe Right((Default0(0.0), Nil, Some(Right("first", First("", 0), Nil))))
    parser[Default0](Seq("first", "arg", "other")) shouldBe Right((Default0(0.0), Nil, Some(Right("first", First("", 0), Seq("arg", "other")))))
    parser[Default0](Seq("first", "--foo", "bah", "--bar", "4")) shouldBe Right((Default0(0.0), Nil, Some(Right("first", First("bah", 4), Nil))))
    parser[Default0](Seq("first", "-f", "bah", "--bar", "4")) shouldBe Right((Default0(0.0), Nil, Some(Right("first", First("bah", 4), Nil))))
    parser[Default0](Seq("--bah", "3", "first")) shouldBe Right((Default0(3.0), Nil, Some(Right("first", First("", 0), Nil))))
    parser[Default0](Seq("second")) shouldBe Right((Default0(0.0), Nil, Some(Right("second", Second("", 0), Nil))))
    parser[Default0](Seq("second", "--baz", "5", "other")) shouldBe Right((Default0(0.0), Nil, Some(Right("second", Second("", 5), Seq("other")))))
    parser[Default0](Seq("second", "--bar", "5", "other")) shouldBe Right((Default0(0.0), Nil, Some(Left("Unrecognized argument: --bar"))))
  }

}

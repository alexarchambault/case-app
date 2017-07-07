package caseapp

import org.scalatest._


class Tests extends FlatSpec with Matchers {
  import Definitions._

  "A parser" should "parse no args" in {
    Parser[NoArgs].apply(Seq.empty) shouldEqual Right((NoArgs(), Seq.empty))
  }

  it should "find an illegal argument" in {
    Parser[NoArgs].parse(Seq("-a")).isLeft shouldBe true
  }

  it should "handle extra user arguments" in {
    Parser[NoArgs].detailedParse(Seq("--", "b", "-a", "--other")) shouldEqual Right((NoArgs(), Seq(), Seq("b", "-a", "--other")))
  }

  it should "give remaining args as is" in {
    Parser[NoArgs].parse(Seq("user arg", "other user arg")) shouldEqual Right((NoArgs(), Seq("user arg", "other user arg")))
  }

  it should "parse no args and return default values and remaining args" in {
    Parser[FewArgs].parse(Seq("user arg", "other user arg")) shouldEqual Right((FewArgs(), Seq("user arg", "other user arg")))
  }

  it should "parse a few args and return a default value and remaining args" in {
    Parser[FewArgs].parse(Seq("user arg", "--num-foo", "4", "other user arg")) shouldEqual Right((FewArgs(numFoo = 4), Seq("user arg", "other user arg")))
  }

  it should "parse a args recursively and return a default value and remaining args" in {
    Parser[MoreArgs].parse(Seq("user arg", "--num-foo", "4", "--count", "other user arg", "--count")) shouldEqual Right((MoreArgs(count = Tag of 2, few = FewArgs(numFoo = 4)), Seq("user arg", "other user arg")))
  }
  
  it should "parse args" in {
    Parser[demo.DemoOptions].parse(Seq("user arg", "--stages", "first", "--value", "Some value", "--verbose", "--verbose", "--verbose", "other user arg", "--stages", "second", "--first")) shouldEqual Right((demo.DemoOptions(first = true, value = Some("Some value"), verbose = Tag of 3, stages = List("first", "second")), Seq("user arg", "other user arg")))
  }

  it should "parse short args" in {
    Parser[demo.DemoOptions].parse(Seq("user arg", "-S", "first", "--value", "Some value", "-v", "-v", "-v", "other user arg", "-S", "second", "--first")) shouldEqual Right((demo.DemoOptions(first = true, value = Some("Some value"), verbose = Tag of 3, stages = List("first", "second")), Seq("user arg", "other user arg")))
  }

  it should "parse list args" in {
    Parser[WithList].parse(Seq("--list", "2", "--list", "5", "extra")) shouldEqual Right((WithList(list = List(2, 5)), Seq("extra")))
  }

  it should "parse semi-colon separated list args" in {
    Parser[WithTaggedList].parse(Seq("--list", "foo", "--list", "bar", "--list", "other", "extra2")) shouldEqual Right((WithTaggedList(list = List("foo", "bar", "other")), Seq("extra2")))
  }

  it should "parse a user-defined argument type" in {
    Parser[WithCustom].parse(Seq("--custom", "a")) shouldEqual Right((WithCustom(custom = Custom("a")), Seq.empty))
  }

  it should "parse first README options" in {
    Parser[ReadmeOptions1].parse(Seq("--user", "aaa", "--enable-foo", "--file", "some_file", "extra_arg", "other_extra_arg")) shouldEqual Right((
      ReadmeOptions1(Some("aaa"), enableFoo = true, List("some_file")),
      Seq("extra_arg", "other_extra_arg")
    ))
  }

  it should "parse first README options (second args example)" in {
    Parser[ReadmeOptions1].parse(Seq("--user", "bbb", "-f", "first_file", "-f", "second_file")) shouldEqual Right((
      ReadmeOptions1(Some("bbb"), enableFoo = false, List("first_file", "second_file")),
      Seq()
    ))
  }

  it should "parse second README options" in {
    Parser[ReadmeOptions2].parse(Seq("--user", "aaa", "--password", "pass", "extra", "-b", "bar")) shouldEqual Right((
      ReadmeOptions2(AuthOptions("aaa", "pass"), PathOptions("", "bar")),
      Seq("extra")
    ))
  }

  it should "parse commands" in {
    sealed trait Command

    case class First(
      @ExtraName("f") foo: String = "",
      bar: Int = 0
    ) extends Command

    case class Second(
      fooh: String = "",
      baz: Int = 0
    ) extends Command

    case class Default0(bah: Double = 0.0)

    val parser = CommandParser[Command]

    parser[Default0](Nil) shouldBe Right((Default0(), Nil, None))
    parser[Default0](Seq("--wrong")) shouldBe Left(s"Unrecognized argument: --wrong")
    parser[Default0](Seq("--bah", "2")) shouldBe Right((Default0(2.0), Nil, None))
    parser[Default0](Seq("--bah", "2", "--", "other", "otherother")) shouldBe Right((Default0(2.0), Seq("other", "otherother"), None))
    parser[Default0](Seq("--bah", "2", "--", "other", "--bah")) shouldBe Right((Default0(2.0), Seq("other", "--bah"), None))
    parser[Default0](Seq("first")) shouldBe Right((Default0(), Nil, Some(Right("first", First("", 0), Nil))))
    parser[Default0](Seq("first", "arg", "other")) shouldBe Right((Default0(), Nil, Some(Right("first", First(), Seq("arg", "other")))))
    parser[Default0](Seq("first", "--foo", "bah", "--bar", "4")) shouldBe Right((Default0(), Nil, Some(Right("first", First("bah", 4), Nil))))
    parser[Default0](Seq("first", "-f", "bah", "--bar", "4")) shouldBe Right((Default0(), Nil, Some(Right("first", First("bah", 4), Nil))))
    parser[Default0](Seq("--bah", "3", "first")) shouldBe Right((Default0(3.0), Nil, Some(Right("first", First(), Nil))))
    parser[Default0](Seq("second")) shouldBe Right((Default0(), Nil, Some(Right("second", Second("", 0), Nil))))
    parser[Default0](Seq("second", "--baz", "5", "other")) shouldBe Right((Default0(), Nil, Some(Right("second", Second("", 5), Seq("other")))))
    parser[Default0](Seq("second", "--bar", "5", "other")) shouldBe Right((Default0(), Nil, Some(Left("Unrecognized argument: --bar"))))
  }

}
package caseapp

import caseapp.core.{Error, Indexed}
import caseapp.core.Error.SeveralErrors
import caseapp.core.help.{Help, WithFullHelp, WithHelp}
import caseapp.demo._
import utest._
import caseapp.core.util.Formatter

object CaseAppTests extends TestSuite {

  import Definitions._
  import CaseAppDefinitions._

  val tests = Tests {

    test("parse no args") {
      val parser: Parser[NoArgs] = Parser.derive
      val res                    = parser.parse(Seq.empty)
      val expectedRes            = Right((NoArgs(), Seq.empty))
      assert(res == expectedRes)
    }

    test("find an illegal argument") {
      val parser: Parser[NoArgs] = Parser.derive
      val res                    = parser.parse(Seq("-a")).isLeft
      assert(res)
    }

    test("handle extra user arguments") {
      val parser: Parser[NoArgs] = Parser.derive
      val res                    = parser.detailedParse(Seq("--", "b", "-a", "--other"))
      val expectedRes = Right((
        NoArgs(),
        RemainingArgs(
          Seq(),
          Seq(
            Indexed(1, 1, "b"),
            Indexed(2, 1, "-a"),
            Indexed(3, 1, "--other")
          )
        )
      ))
      assert(res == expectedRes)
    }

    test("give remaining args as is") {
      val parser: Parser[NoArgs] = Parser.derive
      val res                    = parser.parse(Seq("user arg", "other user arg"))
      val expectedRes            = Right((NoArgs(), Seq("user arg", "other user arg")))
      assert(res == expectedRes)
    }

    test("fail if arg specified multiple times") {
      test {
        val res         = Parser[FewArgs].parse(Seq("--num-foo", "2"))
        val expectedRes = Right((FewArgs(numFoo = 2), Nil))
        assert(res == expectedRes)
      }
      test {
        val res = Parser[FewArgs].parse(Seq("--num-foo", "2", "--num-foo", "3"))
        val expectedRes = Left(Error.ParsingArgument(
          Name("numFoo"),
          Error.ArgumentAlreadySpecified("???", Nil),
          Formatter.DefaultNameFormatter
        ))
        assert(res == expectedRes)
      }

      test {
        val res         = Parser[FewArgs1].parse(Seq("--num-foo", "2", "--num-foo", "3"))
        val expectedRes = Right((FewArgs1(numFoo = Last(3)), Nil))
        assert(res == expectedRes)
      }
    }

    test("fail if arg fails to parse") {
      val res = Parser[FewArgs].parse(Seq("--num-foo", "true"))
      val expectedRes = Left(Error.ParsingArgument(
        Name("numFoo"),
        Error.MalformedValue("integer", "true"),
        Formatter.DefaultNameFormatter
      ))
      assert(res == expectedRes)
    }

    test("parse no args and return default values and remaining args") {
      val res         = Parser[FewArgs].parse(Seq("user arg", "other user arg"))
      val expectedRes = Right((FewArgs(), Seq("user arg", "other user arg")))
      assert(res == expectedRes)
    }

    test("parse a few args and return a default value and remaining args") {
      val res         = Parser[FewArgs].parse(Seq("user arg", "--num-foo", "4", "other user arg"))
      val expectedRes = Right((FewArgs(numFoo = 4), Seq("user arg", "other user arg")))
      assert(res == expectedRes)
    }

    test("parse a args recursively and return a default value and remaining args") {
      val res = Parser[MoreArgs].parse(Seq(
        "user arg",
        "--num-foo",
        "4",
        "--count",
        "other user arg",
        "--count"
      ))
      val expectedRes = Right((
        MoreArgs(count = Tag of 2, few = FewArgs(numFoo = 4)),
        Seq("user arg", "other user arg")
      ))
      assert(res == expectedRes)
    }

    test("parse a args recursively with a prefix added") {
      val res = Parser[RecurseWithPrefix].parse(Seq(
        "--no-prefix",
        "4",
        "--prefix-value",
        "value",
        "--prefix-num-foo",
        "10"
      ))
      val expectedRes = Right((
        RecurseWithPrefix(noPrefix = 4, withPrefix = FewArgs(value = "value", numFoo = 10)),
        Seq.empty
      ))
      assert(res == expectedRes)
    }

    test("parse args") {
      val res = Parser[demo.DemoOptions].parse(Seq(
        "user arg",
        "--stages",
        "first",
        "--value",
        "Some value",
        "--verbose",
        "--verbose",
        "--verbose",
        "other user arg",
        "--stages",
        "second",
        "--first"
      ))
      val expectedRes = Right((
        demo.DemoOptions(
          first = true,
          value = Some("Some value"),
          verbose = Tag of 3,
          stages = List("first", "second")
        ),
        Seq("user arg", "other user arg")
      ))
      assert(res == expectedRes)
    }

    test("parse short args") {
      val res = Parser[demo.DemoOptions].parse(Seq(
        "user arg",
        "-S",
        "first",
        "--value",
        "Some value",
        "-v",
        "-v",
        "-v",
        "other user arg",
        "-S",
        "second",
        "--first"
      ))
      val expectedRes = Right((
        demo.DemoOptions(
          first = true,
          value = Some("Some value"),
          verbose = Tag of 3,
          stages = List("first", "second")
        ),
        Seq("user arg", "other user arg")
      ))
      assert(res == expectedRes)
    }

    test("parse list args") {
      val res         = Parser[WithList].parse(Seq("--list", "2", "--list", "5", "extra"))
      val expectedRes = Right((WithList(list = List(2, 5)), Seq("extra")))
      assert(res == expectedRes)
    }

    test("parse semi-colon separated list args") {
      val res = Parser[WithTaggedList].parse(Seq(
        "--list",
        "foo",
        "--list",
        "bar",
        "--list",
        "other",
        "extra2"
      ))
      val expectedRes = Right((WithTaggedList(list = List("foo", "bar", "other")), Seq("extra2")))
      assert(res == expectedRes)
    }

    test("parse a user-defined argument type") {
      val res         = Parser[WithCustom].parse(Seq("--custom", "a"))
      val expectedRes = Right((WithCustom(custom = Custom("a")), Seq.empty))
      assert(res == expectedRes)
    }

    test("parse first README options") {
      val res = Parser[ReadmeOptions1].parse(Seq(
        "--user",
        "aaa",
        "--enable-foo",
        "--file",
        "some_file",
        "extra_arg",
        "other_extra_arg"
      ))
      val expectedRes = Right((
        ReadmeOptions1(Some("aaa"), enableFoo = true, List("some_file")),
        Seq("extra_arg", "other_extra_arg")
      ))
      assert(res == expectedRes)
    }

    test("parse first README options (second args example)") {
      val res =
        Parser[ReadmeOptions1].parse(Seq("--user", "bbb", "-f", "first_file", "-f", "second_file"))
      val expectedRes = Right((
        ReadmeOptions1(Some("bbb"), enableFoo = false, List("first_file", "second_file")),
        Seq()
      ))
      assert(res == expectedRes)
    }

    test("parse second README options") {
      val res = Parser[ReadmeOptions2].parse(Seq(
        "--user",
        "aaa",
        "--password",
        "pass",
        "extra",
        "-b",
        "bar"
      ))
      val expectedRes = Right((
        ReadmeOptions2(AuthOptions("aaa", "pass"), PathOptions("", "bar")),
        Seq("extra")
      ))
      assert(res == expectedRes)
    }

    test("parse third README options (non mandatory args)") {
      val res = Parser[ReadmeOptions3].parse(Seq("--user", "aaa", "extra", "-b", "bar"))
      val expectedRes = Right((
        ReadmeOptions3(None, PathOptions("", "bar")),
        Seq("extra")
      ))
      assert(res == expectedRes)
    }

    test("parse fourth README options (non mandatory args)") {
      val res = Parser[ReadmeOptions4].parse(Seq("--user", "aaa", "extra", "-b", "bar"))
      val expectedRes = Right((
        ReadmeOptions4(
          Left(Error.RequiredOptionNotSpecified("--password")),
          PathOptions("", "bar")
        ),
        Seq("extra")
      ))
      assert(res == expectedRes)
    }

    test("hyphenize printed missing mandatory arguments") {
      val res         = Parser[ReadmeOptions5].parse(Seq())
      val expectedRes = Left(Error.RequiredOptionNotSpecified("--foo-bar"))
      assert(res == expectedRes)
    }

    test("report all missing mandatory arguments") {
      val res = Parser[Example].parse(Seq())
      val expectedRes = Left(SeveralErrors(
        Error.RequiredOptionNotSpecified("--foo"),
        Seq(Error.RequiredOptionNotSpecified("--bar"))
      ))
      assert(res == expectedRes)
    }

    test("report missing args and unknown args together") {
      val res = Parser[Example].parse(Seq("--foo", "foo", "--baz", "10"))
      val expectedRes = Left(SeveralErrors(
        Error.UnrecognizedArgument("--baz"),
        Seq(Error.RequiredOptionNotSpecified("--bar"))
      ))
      assert(res == expectedRes)
    }

    test("print help despite missing mandatory arguments") {

      val parser = Parser[ReadmeOptions2].withHelp

      val args = Seq("--user", "aaa", "extra", "-b", "bar")

      test {
        val res = parser.parse(args)
        val expectedRes = Right((
          WithHelp(
            usage = false,
            help = false,
            Left(Error.RequiredOptionNotSpecified("--password"))
          ),
          List("extra")
        ))
        assert(res == expectedRes)
      }

      test {
        val res = parser.parse(args :+ "--help")
        val expectedRes = Right((
          WithHelp(
            usage = false,
            help = true,
            Left(Error.RequiredOptionNotSpecified("--password"))
          ),
          List("extra")
        ))
        assert(res == expectedRes)
      }
    }

    test("strip options suffix to get default prog name") {
      val help     = Help[demo.DemoOptions]
      val progName = help.progName
      assert(progName == "demo")
    }

    test("use user defined parser") {
      val res         = Parser[OverriddenParser].parse(Seq("--count", "2"))
      val expectedRes = Right((OverriddenParser(2), Nil))
      assert(res == expectedRes)
    }

    test("handle option of boolean argument") {
      test {
        val res         = Parser[OptBool].parse(Seq("--opt"))
        val expectedRes = Right((OptBool(Some(true)), Nil))
        assert(res == expectedRes)
      }

      test {
        val res         = Parser[OptBool].parse(Seq("--opt", "foo"))
        val expectedRes = Right((OptBool(Some(true)), Seq("foo")))
        assert(res == expectedRes)
      }
    }

    test("don't compute default values when creating parser") {
      val parser = Parser[DefaultsThrow]
    }

    test("ignore unrecognized argument if asked") {
      val parser = Parser[FewArgs]
      test {
        val res =
          parser.detailedParse(Nil, stopAtFirstUnrecognized = false, ignoreUnrecognized = true)
        val expected = Right((FewArgs(), RemainingArgs(Nil, Nil)))
        assert(res == expected)
      }
      test {
        val res = parser.detailedParse(
          Seq("--foo", "bar", "--value", "a"),
          stopAtFirstUnrecognized = false,
          ignoreUnrecognized = true
        )
        val expected = Right((
          FewArgs(value = "a"),
          RemainingArgs(
            Seq(
              Indexed(0, 1, "--foo"),
              Indexed(1, 1, "bar")
            ),
            Nil
          )
        ))
        assert(res == expected)
      }
      test {
        val res = parser.detailedParse(
          Seq("--value", "a", "--other"),
          stopAtFirstUnrecognized = false,
          ignoreUnrecognized = true
        )
        val expected = Right((
          FewArgs(value = "a"),
          RemainingArgs(
            Seq(
              Indexed(2, 1, "--other")
            ),
            Nil
          )
        ))
        assert(res == expected)
      }
      test {
        val res = parser.detailedParse(
          Seq("--value", "a"),
          stopAtFirstUnrecognized = false,
          ignoreUnrecognized = true
        )
        val expected = Right((FewArgs(value = "a"), RemainingArgs(Nil, Nil)))
        assert(res == expected)
      }
      test {
        val res = parser.detailedParse(
          Seq("--value", "a", "--", "--other"),
          stopAtFirstUnrecognized = false,
          ignoreUnrecognized = true
        )
        val expected = Right((
          FewArgs(value = "a"),
          RemainingArgs(
            Nil,
            Seq(
              Indexed(3, 1, "--other")
            )
          )
        ))
        assert(res == expected)
      }
      test {
        val res = parser.detailedParse(
          Seq("foo", "--value", "a"),
          stopAtFirstUnrecognized = false,
          ignoreUnrecognized = true
        )
        val expected = Right((
          FewArgs(value = "a"),
          RemainingArgs(
            Seq(
              Indexed(0, 1, "foo")
            ),
            Nil
          )
        ))
        assert(res == expected)
      }
      test {
        val res = parser.detailedParse(
          Seq("--value", "a", "foo", "--", "--other"),
          stopAtFirstUnrecognized = false,
          ignoreUnrecognized = true
        )
        val expected = Right((
          FewArgs(value = "a"),
          RemainingArgs(
            Seq(
              Indexed(2, 1, "foo")
            ),
            Seq(
              Indexed(4, 1, "--other")
            )
          )
        ))
        assert(res == expected)
      }
    }

    test("stop at first unrecognized argument if asked") {
      val parser = Parser[FewArgs]
      test {
        val res      = parser.detailedParse(Nil, stopAtFirstUnrecognized = true)
        val expected = Right((FewArgs(), RemainingArgs(Nil, Nil)))
        assert(res == expected)
      }
      test {
        val res =
          parser.detailedParse(Seq("--value", "a", "--other"), stopAtFirstUnrecognized = true)
        val expected = Right((
          FewArgs(value = "a"),
          RemainingArgs(
            Seq(
              Indexed(2, 1, "--other")
            ),
            Nil
          )
        ))
        assert(res == expected)
      }
      test {
        val res      = parser.detailedParse(Seq("--value", "a"), stopAtFirstUnrecognized = true)
        val expected = Right((FewArgs(value = "a"), RemainingArgs(Nil, Nil)))
        assert(res == expected)
      }
      test {
        val res =
          parser.detailedParse(Seq("--value", "a", "--", "--other"), stopAtFirstUnrecognized = true)
        val expected = Right((
          FewArgs(value = "a"),
          RemainingArgs(
            Seq(
              Indexed(2, 1, "--"),
              Indexed(3, 1, "--other")
            ),
            Nil
          )
        ))
        assert(res == expected)
      }
      test {
        val res = parser.detailedParse(Seq("foo", "--value", "a"), stopAtFirstUnrecognized = true)
        val expected = Right((
          FewArgs(),
          RemainingArgs(
            Seq(
              Indexed(0, 1, "foo"),
              Indexed(1, 1, "--value"),
              Indexed(2, 1, "a")
            ),
            Nil
          )
        ))
        assert(res == expected)
      }
      test {
        val res = parser.detailedParse(
          Seq("--value", "a", "foo", "--", "--other"),
          stopAtFirstUnrecognized = true
        )
        val expected =
          Right((
            FewArgs(value = "a"),
            RemainingArgs(
              Seq(
                Indexed(2, 1, "foo"),
                Indexed(3, 1, "--"),
                Indexed(4, 1, "--other")
              ),
              Nil
            )
          ))
        assert(res == expected)
      }
    }

    test("parse with custom option formatter") {
      val res =
        Parser[FewArgs]
          .nameFormatter((n: Name) => n.name)
          .detailedParse(
            Seq("--value", "b", "--numFoo", "1")
          )

      val expectedRes =
        Right(
          (FewArgs("b", 1), RemainingArgs(Seq(), Seq()))
        )
      assert(res == expectedRes)
    }

    test("parser withHelp works with custom option formatter") {
      val res =
        Parser[FewArgs]
          .nameFormatter((n: Name) => n.name)
          .withHelp
          .detailedParse(
            Seq("--value", "b", "--numFoo", "1")
          )

      val expectedRes =
        Right(
          (WithHelp(false, false, Right(FewArgs("b", 1))), RemainingArgs(List(), List()))
        )
      assert(res == expectedRes)
    }

    test("disable help") {
      val messageOpt =
        try {
          HasHelp.App.main(Array("--help"))
          None
        }
        catch {
          case ex: HasHelp.Errored =>
            Some(ex.error.message)
        }
      assert(messageOpt.contains("Unrecognized argument: --help"))
    }

    test("accept -help") {
      val res =
        Parser[FewArgs].withHelp.detailedParse(
          Seq("-help")
        )

      val expectedRes =
        Right(
          (WithHelp(false, true, Right(FewArgs())), RemainingArgs(List(), List()))
        )
      assert(res == expectedRes)
    }

    test("accept -help with full help") {
      val res =
        Parser[FewArgs].withFullHelp.detailedParse(
          Seq("-help")
        )

      val expectedRes =
        Right(
          (
            WithFullHelp(WithHelp(false, true, Right(FewArgs())), false),
            RemainingArgs(List(), List())
          )
        )
      assert(res == expectedRes)
    }

    test("keep tags") {
      val args     = DemoOptions.help.args
      val valueArg = args.find(_.name.name == "value").getOrElse(sys.error("value arg not found"))
      val stagesArg =
        args.find(_.name.name == "stages").getOrElse(sys.error("stages arg not found"))

      assert(valueArg.tags == Seq(Tag("foo")))
      assert(stagesArg.tags == Seq(Tag("foo"), Tag("other")))
    }

  }

}

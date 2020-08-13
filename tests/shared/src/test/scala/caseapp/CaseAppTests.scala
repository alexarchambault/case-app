package caseapp

import _root_.cats.data.NonEmptyList
import caseapp.core.Error
import caseapp.core.Error.SeveralErrors
import caseapp.core.help.{Help, WithHelp}
import caseapp.demo._
import shapeless.{Inl, Inr}
import utest._
import caseapp.core.util.Formatter
import caseapp.cats.CatsArgParser._

object CaseAppTests extends TestSuite {

  import Definitions._


  val tests = Tests {

    "parse no args" - {
      val res = Parser[NoArgs].parse(Seq.empty)
      val expectedRes = Right((NoArgs(), Seq.empty))
      assert(res == expectedRes)
    }

    "find an illegal argument" - {
      val res = Parser[NoArgs].parse(Seq("-a")).isLeft
      assert(res)
    }

    "handle extra user arguments" - {
      val res = Parser[NoArgs].detailedParse(Seq("--", "b", "-a", "--other"))
      val expectedRes = Right((NoArgs(), RemainingArgs(Seq(), Seq("b", "-a", "--other"))))
      assert(res == expectedRes)
    }

    "give remaining args as is" - {
      val res = Parser[NoArgs].parse(Seq("user arg", "other user arg"))
      val expectedRes = Right((NoArgs(), Seq("user arg", "other user arg")))
      assert(res == expectedRes)
    }

    "fail if arg specified multiple times" - {
      * - {
        val res = Parser[FewArgs].parse(Seq("--num-foo", "2"))
        val expectedRes = Right((FewArgs(numFoo = 2), Nil))
        assert(res == expectedRes)
      }
      * - {
        val res = Parser[FewArgs].parse(Seq("--num-foo", "2", "--num-foo", "3"))
        val expectedRes = Left(Error.ParsingArgument(Name("numFoo"), Error.ArgumentAlreadySpecified("???", Nil), Formatter.DefaultNameFormatter))
        assert(res == expectedRes)
      }

      * - {
        val res = Parser[FewArgs1].parse(Seq("--num-foo", "2", "--num-foo", "3"))
        val expectedRes = Right((FewArgs1(numFoo = Last(3)), Nil))
        assert(res == expectedRes)
      }
    }

    "fail if arg fails to parse" - {
      val res = Parser[FewArgs].parse(Seq("--num-foo", "true"))
      val expectedRes = Left(Error.ParsingArgument(Name("numFoo"), Error.MalformedValue("integer", "true"), Formatter.DefaultNameFormatter))
      assert(res == expectedRes)
    }

    "parse no args and return default values and remaining args" - {
      val res = Parser[FewArgs].parse(Seq("user arg", "other user arg"))
      val expectedRes = Right((FewArgs(), Seq("user arg", "other user arg")))
      assert(res == expectedRes)
    }

    "parse a few args and return a default value and remaining args" - {
      val res = Parser[FewArgs].parse(Seq("user arg", "--num-foo", "4", "other user arg"))
      val expectedRes = Right((FewArgs(numFoo = 4), Seq("user arg", "other user arg")))
      assert(res == expectedRes)
    }

    "parse a args recursively and return a default value and remaining args" - {
      val res = Parser[MoreArgs].parse(Seq("user arg", "--num-foo", "4", "--count", "other user arg", "--count"))
      val expectedRes = Right((MoreArgs(count = Tag of 2, few = FewArgs(numFoo = 4)), Seq("user arg", "other user arg")))
      assert(res == expectedRes)
    }

    "parse args" - {
      val res = Parser[demo.DemoOptions].parse(Seq("user arg", "--stages", "first", "--value", "Some value", "--verbose", "--verbose", "--verbose", "other user arg", "--stages", "second", "--first"))
      val expectedRes = Right((demo.DemoOptions(first = true, value = Some("Some value"), verbose = Tag of 3, stages = List("first", "second")), Seq("user arg", "other user arg")))
      assert(res == expectedRes)
    }

    "parse short args" - {
      val res = Parser[demo.DemoOptions].parse(Seq("user arg", "-S", "first", "--value", "Some value", "-v", "-v", "-v", "other user arg", "-S", "second", "--first"))
      val expectedRes = Right((demo.DemoOptions(first = true, value = Some("Some value"), verbose = Tag of 3, stages = List("first", "second")), Seq("user arg", "other user arg")))
      assert(res == expectedRes)
    }

    "parse list args" - {
      val res = Parser[WithList].parse(Seq("--list", "2", "--list", "5", "extra"))
      val expectedRes = Right((WithList(list = List(2, 5)), Seq("extra")))
      assert(res == expectedRes)
    }

    "parse semi-colon separated list args" - {
      val res = Parser[WithTaggedList].parse(Seq("--list", "foo", "--list", "bar", "--list", "other", "extra2"))
      val expectedRes = Right((WithTaggedList(list = List("foo", "bar", "other")), Seq("extra2")))
      assert(res == expectedRes)
    }

    "parse nonEmptyList args" - {
      val res =
        Parser[WithNonEmptyList].parse(Seq("--nel", "2", "--nel", "5", "extra"))
      val expectedRes =
        Right((WithNonEmptyList(nel = NonEmptyList.of("2", "5")), Seq("extra")))
      assert(res == expectedRes)
    }

    "parse a user-defined argument type" - {
      val res = Parser[WithCustom].parse(Seq("--custom", "a"))
      val expectedRes = Right((WithCustom(custom = Custom("a")), Seq.empty))
      assert(res == expectedRes)
    }

    "parse first README options" - {
      val res = Parser[ReadmeOptions1].parse(Seq("--user", "aaa", "--enable-foo", "--file", "some_file", "extra_arg", "other_extra_arg"))
      val expectedRes = Right((
        ReadmeOptions1(Some("aaa"), enableFoo = true, List("some_file")),
        Seq("extra_arg", "other_extra_arg")
      ))
      assert(res == expectedRes)
    }

    "parse first README options (second args example)" - {
      val res = Parser[ReadmeOptions1].parse(Seq("--user", "bbb", "-f", "first_file", "-f", "second_file"))
      val expectedRes = Right((
        ReadmeOptions1(Some("bbb"), enableFoo = false, List("first_file", "second_file")),
        Seq()
      ))
      assert(res == expectedRes)
    }

    "parse second README options" - {
      val res = Parser[ReadmeOptions2].parse(Seq("--user", "aaa", "--password", "pass", "extra", "-b", "bar"))
      val expectedRes = Right((
        ReadmeOptions2(AuthOptions("aaa", "pass"), PathOptions("", "bar")),
        Seq("extra")
      ))
      assert(res == expectedRes)
    }

    "parse third README options (non mandatory args)" - {
      val res = Parser[ReadmeOptions3].parse(Seq("--user", "aaa", "extra", "-b", "bar"))
      val expectedRes = Right((
        ReadmeOptions3(None, PathOptions("", "bar")),
        Seq("extra")
      ))
      assert(res == expectedRes)
    }

    "parse fourth README options (non mandatory args)" - {
      val res = Parser[ReadmeOptions4].parse(Seq("--user", "aaa", "extra", "-b", "bar"))
      val expectedRes = Right((
        ReadmeOptions4(Left(Error.RequiredOptionNotSpecified("--password")), PathOptions("", "bar")),
        Seq("extra")
      ))
      assert(res == expectedRes)
    }

    "hyphenize printed missing mandatory arguments" - {
      val res = Parser[ReadmeOptions5].parse(Seq())
      val expectedRes = Left(Error.RequiredOptionNotSpecified("--foo-bar"))
      assert(res == expectedRes)
    }

    "report all missing mandatory arguments" - {
      val res = Parser[Example].parse(Seq())
      val expectedRes = Left(SeveralErrors(Error.RequiredOptionNotSpecified("--foo"), Seq(Error.RequiredOptionNotSpecified("--bar"))))
      assert(res == expectedRes)
    }

    "report missing args and unknown args together" - {
      val res = Parser[Example].parse(Seq("--foo", "foo", "--baz", "10"))
      val expectedRes = Left(SeveralErrors(Error.UnrecognizedArgument("--baz"), Seq(Error.RequiredOptionNotSpecified("--bar"))))
      assert(res == expectedRes)
    }

    "print help despite missing mandatory arguments" - {

      val parser = Parser[ReadmeOptions2].withHelp

      val args = Seq("--user", "aaa", "extra", "-b", "bar")

      * - {
        val res = parser.parse(args)
        val expectedRes = Right((
          WithHelp(usage = false, help = false, Left(Error.RequiredOptionNotSpecified("--password"))),
          List("extra")
        ))
        assert(res == expectedRes)
      }

      * - {
        val res = parser.parse(args :+ "--help")
        val expectedRes = Right((
          WithHelp(usage = false, help = true, Left(Error.RequiredOptionNotSpecified("--password"))),
          List("extra")
        ))
        assert(res == expectedRes)
      }
    }

    "strip options suffix to get default prog name" - {
      val help = Help[demo.DemoOptions]
      val progName = help.progName
      assert(progName == "demo")
    }

    "parse commands" - {

      val parser = CommandParser[Command]

      * - {
        val res = parser.parse[Default0](Nil)
        val expectedRes = Right((Default0(), Nil, None))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("--wrong"))
        val expectedRes = Left(Error.UnrecognizedArgument("--wrong"))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("--bah", "2"))
        val expectedRes = Right((Default0(2.0), Nil, None))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("--bah", "2", "--", "other", "otherother"))
        val expectedRes = Right((Default0(2.0), Seq("other", "otherother"), None))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("--bah", "2", "--", "other", "--bah"))
        val expectedRes = Right((Default0(2.0), Seq("other", "--bah"), None))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("first"))
        val expectedRes = Right((Default0(), Nil, Some(Right(Seq("first"), First("", 0), Nil))))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("first", "arg", "other"))
        val expectedRes = Right((Default0(), Nil, Some(Right(Seq("first"), First(), Seq("arg", "other")))))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("first", "--foo", "bah", "--bar", "4"))
        val expectedRes = Right((Default0(), Nil, Some(Right(Seq("first"), First("bah", 4), Nil))))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("first", "-f", "bah", "--bar", "4"))
        val expectedRes = Right((Default0(), Nil, Some(Right(Seq("first"), First("bah", 4), Nil))))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("--bah", "3", "first"))
        val expectedRes = Right((Default0(3.0), Nil, Some(Right(Seq("first"), First(), Nil))))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("second"))
        val expectedRes = Right((Default0(), Nil, Some(Right(Seq("second"), Second("", 0), Nil))))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("second", "--baz", "5", "other"))
        val expectedRes = Right((Default0(), Nil, Some(Right(Seq("second"), Second("", 5), Seq("other")))))
        assert(res == expectedRes)
      }
      * - {
        val res = parser.parse[Default0](Seq("second", "--bar", "5", "other"))
        val expectedRes = Right((Default0(), Nil, Some(Left(Error.UnrecognizedArgument("--bar")))))
        assert(res == expectedRes)
      }
    }

    "parse manually defined command" - {
      "adt" - {
        * - {
          val res = ManualCommand.commandParser.parse[Default0](Seq("c1", "-s", "aa"))
          val expectedRes = Right((Default0(), Nil, Some(Right(Seq("c1"), Command1Opts("aa"), Nil))))
          assert(res == expectedRes)
        }

        * - {
          val res = ManualCommand.commandParser.parse[Default0](Seq("c2", "-b"))
          val expectedRes = Right((Default0(), Nil, Some(Right(Seq("c2"), Command2Opts(true), Nil))))
          assert(res == expectedRes)
        }

        "find the user-specified name of a command arguments" - {
          ManualCommand.commandsMessages.messagesMap.get(Seq("c1")).exists { h =>
            h.argsNameOption.exists(_ == "c1-stuff")
          }
        }
      }

      "not adt" - {
        * - {
          val res = ManualCommandNotAdt.commandParser.parse[Default0](Seq("c1", "-s", "aa"))
          val expectedRes = Right((Default0(), Nil, Some(Right((Seq("c1"), Inl(ManualCommandNotAdtOptions.Command1Opts("aa")), Nil)))))
          assert(res == expectedRes)
        }

        * - {
          val res = ManualCommandNotAdt.commandParser.parse[Default0](Seq("c2", "-b"))
          val expectedRes = Right((Default0(), Nil, Some(Right((Seq("c2"), Inr(Inl(ManualCommandNotAdtOptions.Command2Opts(true))), Nil)))))
          assert(res == expectedRes)
        }

        "find the user-specified name of a command arguments" - {
          ManualCommandNotAdt.commandsMessages.messagesMap.get(Seq("c1")).exists { h =>
            h.argsNameOption.exists(_ == "c1-stuff")
          }
        }

        "stop at first unrecognized argument if asked so" - {
          * - {
            val res = ManualCommandNotAdt.commandParser.parse[Default0](Seq("c3", "-b"))
            val expectedRes = Right((Default0(), Nil, Some(Right((Seq("c3"), Inr(Inr(Inl(ManualCommandNotAdtOptions.Command3Opts()))), Seq("-b"))))))
            assert(res == expectedRes)
          }

          * - {
            val res = ManualCommandNotAdt.commandParser.parse[Default0](Seq("c3", "-n", "1", "--foo"))
            val expectedRes = Right((Default0(), Nil, Some(Right((Seq("c3"), Inr(Inr(Inl(ManualCommandNotAdtOptions.Command3Opts(1)))), Seq("--foo"))))))
            assert(res == expectedRes)
          }
        }

        "parser with custom name formatter" - {
          val res = ManualCommandNotAdt.commandParser.parse[Default0](Seq("c4", "--someString", "aa"))
          val expectedRes = Right((Default0(), Nil, Some(Right((Seq("c4"), Inr(Inr(Inr(Inl(ManualCommandNotAdtOptions.Command4Opts("aa"))))), Nil)))))
          assert(res == expectedRes)
        }
      }

      "sub commands" - {
        * - {
          val res = ManualSubCommand.commandParser.parse[Default0](Seq("foo", "-s", "aa"))
          val expectedRes = Right((Default0(), Nil, Some(Right((Seq("foo"), Inl(ManualSubCommandOptions.Command1Opts("aa")), Nil)))))
          assert(res == expectedRes)
        }

        * - {
          val res = ManualSubCommand.commandParser.parse[Default0](Seq("foo", "list", "-b"))
          val expectedRes = Right((Default0(), Nil, Some(Right((Seq("foo", "list"), Inr(Inl(ManualSubCommandOptions.Command2Opts(true))), Nil)))))
          assert(res == expectedRes)
        }

        "find the user-specified name of a command arguments" - {
          ManualSubCommand.commandsMessages.messagesMap.get(Seq("foo")).exists { h =>
            h.argsNameOption.exists(_ == "c1-stuff")
          }
        }
      }
    }

    "use user defined parser" - {
      val res = Parser[OverriddenParser].parse(Seq("--count", "2"))
      val expectedRes = Right((OverriddenParser(2), Nil))
      assert(res == expectedRes)
    }

    "handle option of boolean argument" - {
      * - {
        val res = Parser[OptBool].parse(Seq("--opt"))
        val expectedRes = Right((OptBool(Some(true)), Nil))
        assert(res == expectedRes)
      }

      * - {
        val res = Parser[OptBool].parse(Seq("--opt", "foo"))
        val expectedRes = Right((OptBool(Some(true)), Seq("foo")))
        assert(res == expectedRes)
      }
    }

    "don't compute default values when creating parser" - {
      caseapp.util.Default[DefaultsThrow]
      shapeless.lazily[caseapp.util.Default.AsOptions[DefaultsThrow]]
      val parser = Parser[DefaultsThrow]
    }

    "stop at first unrecognized argument if asked" - {
      val parser = Parser[FewArgs]
      * - {
        val res = parser.detailedParse(Nil, stopAtFirstUnrecognized = true)
        val expected = Right((FewArgs(), RemainingArgs(Nil, Nil)))
        assert(res == expected)
      }
      * - {
        val res = parser.detailedParse(Seq("--value", "a", "--other"), stopAtFirstUnrecognized = true)
        val expected = Right((FewArgs(value = "a"), RemainingArgs(Seq("--other"), Nil)))
        assert(res == expected)
      }
      * - {
        val res = parser.detailedParse(Seq("--value", "a"), stopAtFirstUnrecognized = true)
        val expected = Right((FewArgs(value = "a"), RemainingArgs(Nil, Nil)))
        assert(res == expected)
      }
      * - {
        val res = parser.detailedParse(Seq("--value", "a", "--", "--other"), stopAtFirstUnrecognized = true)
        val expected = Right((FewArgs(value = "a"), RemainingArgs(Seq("--", "--other"), Nil)))
        assert(res == expected)
      }
      * - {
        val res = parser.detailedParse(Seq("foo", "--value", "a"), stopAtFirstUnrecognized = true)
        val expected = Right((FewArgs(), RemainingArgs(Seq("foo", "--value", "a"), Nil)))
        assert(res == expected)
      }
      * - {
        val res = parser.detailedParse(Seq("--value", "a", "foo", "--", "--other"), stopAtFirstUnrecognized = true)
        val expected = Right((FewArgs(value = "a"), RemainingArgs(Seq("foo", "--", "--other"), Nil)))
        assert(res == expected)
      }
    }

    "parse with custom option formatter" - {
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

    "parser withHelp works with custom option formatter" - {
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

  }

}

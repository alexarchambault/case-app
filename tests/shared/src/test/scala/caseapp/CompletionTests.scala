package caseapp

import caseapp.core.complete.{Bash, CompletionItem, Fish, Zsh}
import utest._

object CompletionTests extends TestSuite {

  // added by CaseApp#complete to the options of the applications
  private val helpItems = List(
    CompletionItem("--usage", Some("Print usage and exit"), Nil),
    CompletionItem("--help", Some("Print help message and exit"), List("-h"))
  )

  val tests = Tests {
    test("simple") {
      import CompletionDefinitions.Simple._
      test {
        val res = App.complete(Seq("-"), 0)
        val expected = List(
          CompletionItem("--value")
        ) ++ helpItems
        assert(res == expected)
      }
      test {
        val res = App.complete(Seq("--v"), 0)
        val expected = List(
          CompletionItem("--value")
        )
        assert(res == expected)
      }
      test {
        val res      = App.complete(Seq("--a"), 0)
        val expected = Nil
        assert(res == expected)
      }

      test {
        val res = App.complete(Seq("foo", "a", "--v"), 2)
        val expected = List(
          CompletionItem("--value")
        )
        assert(res == expected)
      }
    }

    test("multiple") {
      import CompletionDefinitions.Multiple._
      test {
        val res = App.complete(Seq("-"), 0)
        val expected = List(
          CompletionItem("--value", Some("A value"), List("-V")),
          CompletionItem("--other", None, List("-n"))
        ) ++ helpItems
        assert(res == expected)
      }
      test {
        val res = App.complete(Seq("--v"), 0)
        val expected = List(
          CompletionItem("--value", Some("A value"), Nil)
        )
        assert(res == expected)
      }
      test {
        val res      = App.complete(Seq("--a"), 0)
        val expected = Nil
        assert(res == expected)
      }

      test {
        val res = App.complete(Seq("aa", "./f", "--v"), 2)
        val expected = List(
          CompletionItem("--value", Some("A value"), Nil)
        )
        assert(res == expected)
      }
    }

    test("argument value") {
      import CompletionDefinitions.ArgCompletion._
      test {
        val res = App.complete(Seq("-"), 0)
        val expected = List(
          CompletionItem("--value", Some("A value"), List("-V")),
          CompletionItem("--other", None, List("-n"))
        ) ++ helpItems
        assert(res == expected)
      }
      test {
        val res = App.complete(Seq("--v"), 0)
        val expected = List(
          CompletionItem("--value", Some("A value"), Nil)
        )
        assert(res == expected)
      }
      test {
        val res = App.complete(Seq("-n", "32", "--value", ""), 3)
        val expected = List(
          CompletionItem("32000", None, Nil),
          CompletionItem("32001", None, Nil),
          CompletionItem("32002", None, Nil)
        )
        assert(res == expected)
      }
      test {
        val res = App.complete(Seq("-n", "41", "--value", ""), 3)
        val expected = List(
          CompletionItem("41000", None, Nil),
          CompletionItem("41001", None, Nil),
          CompletionItem("41002", None, Nil)
        )
        assert(res == expected)
      }
    }

    test("commands") {
      import CompletionDefinitions.Commands._
      test {
        val res = Prog.complete(Seq(""), 0)
        val expected = List(
          CompletionItem("back-tick", None, Nil),
          CompletionItem("first", None, Nil),
          CompletionItem("second", None, Nil)
        )
        assert(res == expected)
      }
      test {
        val res = Prog.complete(Seq("f"), 0)
        val expected = List(
          CompletionItem("first", None, Nil)
        )
        assert(res == expected)
      }

      test {
        val res = Prog.complete(Seq("first", "-"), 1)
        val expected = List(
          CompletionItem("--value", Some("A value"), List("-V")),
          CompletionItem("--other", None, List("-n"))
        ) ++ helpItems
        assert(res == expected)
      }

      test {
        val res = Prog.complete(Seq("second", "-"), 1)
        val expected = List(
          CompletionItem("--glob", Some("A pattern"), List("-g")),
          CompletionItem("--count", None, List("-d"))
        ) ++ helpItems
        assert(res == expected)
      }

      test("bash") {
        val res = Prog.complete(Seq("back-tick", "-"), 1)
        val expected = List(
          CompletionItem("--backtick", Some("A pattern with backtick `--`\nwith multiline"), Nil),
          CompletionItem("--count", None, List("-d"))
        ) ++ helpItems
        assert(res == expected)

        val compRely         = Bash.print(res)
        val expectedCompRely = "'--backtick  -- A pattern with backtick `--`'"

        assert(compRely.contains(expectedCompRely))
      }
      test("bash fallback to file completion") {
        val compRely = Bash.print(Nil)
        assert(compRely.contains("compopt -o filenames"))
        assert(compRely.contains("""compgen -f -- "$cur""""))
      }
      test("fish") {
        val res              = Prog.complete(Seq("back-tick", "-"), 1)
        val compRely         = Fish.print(res)
        val expectedCompRely = "--backtick\tA pattern with backtick `--`\n".stripMargin

        assert(compRely.contains(expectedCompRely))
      }
      test("zsh") {
        val res = Prog.complete(Seq("back-tick", "-"), 1)
        val expected = List(
          CompletionItem("--backtick", Some("A pattern with backtick `--`\nwith multiline"), Nil),
          CompletionItem("--count", None, List("-d"))
        ) ++ helpItems
        assert(res == expected)

        val compRely         = Zsh.print(res)
        val expectedCompRely = "'--backtick:A pattern with backtick `--`'"

        assert(compRely.contains(expectedCompRely))
      }
    }

    test("commands with default") {
      import CompletionDefinitions.CommandsWithDefault._
      test {
        val res = Prog.complete(Seq(""), 0)
        val expected = List(
          CompletionItem("first", None, Nil),
          CompletionItem("second", None, Nil)
        )
        assert(res == expected)
      }
      test {
        val res = Prog.complete(Seq("f"), 0)
        val expected = List(
          CompletionItem("first", None, Nil)
        )
        assert(res == expected)
      }

      test {
        val res = Prog.complete(Seq("-"), 0)
        val expected = List(
          CompletionItem("--value", Some("A value"), List("-V")),
          CompletionItem("--other", None, List("-n"))
        ) ++ helpItems
        assert(res == expected)
      }

      test {
        val res = Prog.complete(Seq("first", "-"), 1)
        val expected = List(
          CompletionItem("--value", Some("A value"), List("-V")),
          CompletionItem("--other", None, List("-n"))
        ) ++ helpItems
        assert(res == expected)
      }

      test {
        val res = Prog.complete(Seq("second", "-"), 1)
        val expected = List(
          CompletionItem("--glob", Some("A pattern"), List("-g")),
          CompletionItem("--count", None, List("-d"))
        ) ++ helpItems
        assert(res == expected)
      }
    }

    test("hidden commands and options") {
      import CompletionDefinitions.HiddenAndStopping._
      test {
        val res = Prog.complete(Seq(""), 0)
        val expected = List(
          CompletionItem("stopping", None, Nil),
          CompletionItem("visible", None, Nil)
        )
        assert(res == expected)
      }
      test {
        val res      = Prog.complete(Seq("se"), 0)
        val expected = Nil
        assert(res == expected)
      }
      test("options of hidden commands are still completed") {
        val res = Prog.complete(Seq("secret", "--v"), 1)
        val expected = List(
          CompletionItem("--value", Some("A value"), Nil)
        )
        assert(res == expected)
      }
      test {
        val res      = Prog.complete(Seq("visible", "--s"), 1)
        val expected = Nil
        assert(res == expected)
      }
    }

    test("stop at first unrecognized") {
      import CompletionDefinitions.HiddenAndStopping._
      test {
        val res = Prog.complete(Seq("stopping", "-"), 1)
        val expected = List(
          CompletionItem("--value", Some("A value"), List("-V"))
        ) ++ helpItems
        assert(res == expected)
      }
      test {
        val res = Prog.complete(Seq("stopping", "--value", "a", "-"), 3)
        val expected = List(
          CompletionItem("--value", Some("A value"), List("-V"))
        ) ++ helpItems
        assert(res == expected)
      }
      test("not after the first unrecognized argument") {
        val res      = Prog.complete(Seq("stopping", "foo", "-"), 2)
        val expected = Nil
        assert(res == expected)
      }
    }
  }
}

package caseapp

import caseapp.core.app.CommandsEntryPoint
import caseapp.core.help.{Help, HelpFormat, RuntimeCommandHelp, RuntimeCommandsHelp}
import utest._

object HelpTests extends TestSuite {

  import Definitions.{Command => _, First => _, Second => _, Third => _, _}
  import HelpDefinitions._

  case class Options(
    first: Int,
    @Hidden
    second: String
  )

  case class WithValueDescription(
    @ValueDescription("overridden description") value: String
  )

  val format = HelpFormat.default(false)

  val tests = Tests {

    def lines(s: String) = s.linesIterator.toVector
    def checkLines(message: String, expectedMessage: String) = {
      val messageLines  = lines(message)
      val expectedLines = lines(expectedMessage)

      for (((a, b), idx) <- messageLines.zip(expectedLines).zipWithIndex if a != b)
        Console.err.println(s"Line $idx, expected '$b', got '$a'")

      assert(messageLines == expectedLines)
    }

    test("generate a help message") {

      val message = Help[Example].help(format)

      val expectedMessage =
        """Usage: example [options]
          |
          |Options:
          |  --foo string
          |  --bar int""".stripMargin

      checkLines(message, expectedMessage)
    }

    test("generate a help message with description") {

      val message = Help[ExampleWithHelpMessage].help(format)

      val expectedMessage =
        """Usage: example-with-help-message [options]
          |Example help message
          |
          |Options:
          |  --foo string
          |  --bar int""".stripMargin

      checkLines(message, expectedMessage)
    }

    test("group options") {

      val orderedGroups = Seq("Something", "Bb").zipWithIndex.toMap
      val groupFormat = format.withSortGroups(Some { groups =>
        groups.sortBy(g => orderedGroups.getOrElse(g, Int.MaxValue))
      })
      val message = Help[GroupedOptions].help(groupFormat)

      val expectedMessage =
        """Usage: grouped [options]
          |Example help message
          |
          |Something options:
          |  --foo string
          |  --other double
          |
          |Bb options:
          |  --bar int
          |  --something""".stripMargin

      checkLines(message, expectedMessage)
    }

    test("hidden group of options") {

      val orderedGroups = Seq("Something", "Bb").zipWithIndex.toMap
      val groupFormat = format.withSortGroups(Some { groups =>
        groups.sortBy(g => orderedGroups.getOrElse(g, Int.MaxValue))
      })
      val message = Help[HiddenGroupOptions].help(groupFormat)

      val expectedMessage =
        """Usage: hidden-group [options]
          |Example help message
          |
          |Something options:
          |  --foo string
          |  --other double""".stripMargin

      checkLines(message, expectedMessage)
    }

    test("group ordering") {

      val orderedGroups = Seq("Something", "Bb")
      val groupFormat   = format.withSortedGroups(Some(orderedGroups))
      val message       = Help[GroupedOptions].help(groupFormat)

      val expectedMessage =
        """Usage: grouped [options]
          |Example help message
          |
          |Something options:
          |  --foo string
          |  --other double
          |
          |Bb options:
          |  --bar int
          |  --something""".stripMargin

      checkLines(message, expectedMessage)
    }

    test("mark optional options with ? in help messages") {
      val message = Help[OptBool].help(format)

      val expectedMessage =
        """Usage: opt-bool [options]
          |
          |Options:
          |  --opt""".stripMargin

      checkLines(message, expectedMessage)
    }

    test("mark repeatable options with * in help messages") {
      val message = Help[WithList].help(format)

      val expectedMessage =
        """Usage: with-list [options]
          |
          |Options:
          |  --list int*""".stripMargin

      checkLines(message, expectedMessage)
    }

    test("use custom arg parser descriptions in help messages") {
      val message = Help[WithCustom].help(format)

      val expectedMessage =
        """Usage: with-custom [options]
          |
          |Options:
          |  --custom custom parameter""".stripMargin

      checkLines(message, expectedMessage)
    }

    test("use value descriptions from annotations when given") {
      val message = Help[WithValueDescription].help(format)

      val expectedMessage =
        """Usage: with-value-description [options]
          |
          |Options:
          |  --value overridden description""".stripMargin

      checkLines(message, expectedMessage)
    }

    test("don't add a help message for fields annotated with @Hidden") {

      val helpLines = Help[Options].help(format).linesIterator.toVector

      test {
        val res         = helpLines.count(_.contains("--first"))
        val expectedRes = 1
        assert(res == expectedRes)
      }
      test {
        val res         = helpLines.count(_.contains("--second"))
        val expectedRes = 0
        assert(res == expectedRes)
      }
    }

    test("add a help message for fields annotated with @Hidden in full help") {

      val helpLines = Help[Options].help(format, showHidden = true).linesIterator.toVector

      test {
        val res         = helpLines.count(_.contains("--first"))
        val expectedRes = 1
        assert(res == expectedRes)
      }
      test {
        val res         = helpLines.count(_.contains("--second"))
        val expectedRes = 1
        assert(res == expectedRes)
      }
    }

    test("duplicates") {

      import Definitions.Duplicates._

      test {
        val dup = Help[Foo].duplicates
        assert(dup.nonEmpty)

        val expected = Map("--foo-bar" -> Seq("fooBar", "foo-bar"))
        val res      = dup.mapValues(_.map(_.name.name)).iterator.toMap
        assert(res == expected)
      }

      test {
        val dup = Help[Bar].duplicates
        assert(dup.nonEmpty)

        val expected = Map("--foo-bar" -> Seq("fooBar", "other"))
        val res      = dup.mapValues(_.map(_.name.name)).iterator.toMap
        assert(res == expected)
      }

      test {
        val dup = Help[Second].duplicates
        assert(dup.nonEmpty)

        val expected = Map("--foo-bar" -> Seq("fooBar", "foo-bar"))
        val res      = dup.mapValues(_.map(_.name.name)).iterator.toMap
        assert(res == expected)
      }

    }

    test("generate a help message with custom formatter") {

      implicit val p = Parser[FewArgs].nameFormatter((n: Name) => n.name)
      val message    = Help[FewArgs].help(format)

      val expectedMessage =
        """Usage: few-args [options]
          |
          |Options:
          |  --value string
          |  --numFoo int""".stripMargin

      checkLines(message, expectedMessage)
    }

    test("generate help message for commands (legacy API)") {
      val msg = CommandTest.commandsMessages
        .messagesMap(List("third"))
        .helpMessage
        .map(_.message)
        .getOrElse("")
      val expected = "Third help message"

      assert(msg == expected)
    }

    test("generate help message for commands") {
      val entryPoint = new CommandsEntryPoint {
        def progName                = "foo"
        override def defaultCommand = Some(First)
        def commands                = Seq(First, Second, Third)
      }
      val help = entryPoint.help.help(format)
      val expected =
        """Usage: foo <COMMAND> [options]
          |
          |Help options:
          |  --usage            Print usage and exit
          |  -h, -help, --help  Print help message and exit
          |
          |Other options:
          |  -f, --foo string
          |  --bar int
          |
          |Commands:
          |  first
          |  second
          |  third   Third help message""".stripMargin

      assert(help == expected)
    }

    test("group commands in help message") {
      val entryPoint = new CommandsEntryPoint {
        def progName                = "foo"
        override def defaultCommand = Some(CommandGroups.First)
        def commands = Seq(CommandGroups.First, CommandGroups.Second, CommandGroups.Third)
      }
      val help = entryPoint.help.help(format)
      val expected =
        """Usage: foo <COMMAND> [options]
          |
          |Help options:
          |  --usage            Print usage and exit
          |  -h, -help, --help  Print help message and exit
          |
          |Other options:
          |  -f, --foo string
          |  --bar int
          |
          |Aa commands:
          |  first
          |  third  Third help message
          |
          |Bb commands:
          |  second""".stripMargin

      assert(help == expected)
    }
    test("empty help in help message") {
      val entryPoint = new CommandsEntryPoint {
        def progName                = "foo"
        override def defaultCommand = Some(CommandGroups.First)
        def commands = Seq(CommandGroups.First, CommandGroups.Second, CommandGroups.Third)

        override def help: RuntimeCommandsHelp = new RuntimeCommandsHelp(
          progName,
          None,
          Help[Unit](),
          commands.map(cmd =>
            new RuntimeCommandHelp(cmd.names, cmd.finalHelp, cmd.group, cmd.hidden)
          ),
          None
        )
      }
      val help = entryPoint.help.help(format)
      val expected =
        """Usage: foo <COMMAND>
          |
          |Aa commands:
          |  first
          |  third  Third help message
          |
          |Bb commands:
          |  second""".stripMargin

      assert(help == expected)
    }
    test("help message with summary description") {
      val entryPoint = new CommandsEntryPoint {
        def progName                = "foo"
        override def defaultCommand = Some(CommandGroups.First)
        def commands = Seq(CommandGroups.First, CommandGroups.Second, CommandGroups.Third)

        override def help: RuntimeCommandsHelp = new RuntimeCommandsHelp(
          progName,
          Some("Description"),
          Help[Unit](),
          commands.map(cmd =>
            new RuntimeCommandHelp(cmd.names, cmd.finalHelp, cmd.group, cmd.hidden)
          ),
          Some("Summary Description")
        )
      }
      val help = entryPoint.help.help(format)
      val expected =
        """Usage: foo <COMMAND>
          |Description
          |
          |Aa commands:
          |  first
          |  third  Third help message
          |
          |Bb commands:
          |  second
          |
          |Summary Description""".stripMargin

      assert(help == expected)
    }
    test("help message with hidden group") {
      val entryPoint = new CommandsEntryPoint {
        def progName                = "foo"
        override def defaultCommand = Some(CommandGroups.First)
        def commands = Seq(CommandGroups.First, CommandGroups.Second, CommandGroups.Third)
      }
      val formatWithHiddenGroup = format.withHiddenGroups(Some(Seq(
        CommandGroups.First.group,
        CommandGroups.Third.group
      )))
      val help = entryPoint.help.help(formatWithHiddenGroup)
      val expected =
        """Usage: foo <COMMAND> [options]
          |
          |Help options:
          |  --usage            Print usage and exit
          |  -h, -help, --help  Print help message and exit
          |
          |Other options:
          |  -f, --foo string
          |  --bar int
          |
          |Bb commands:
          |  second""".stripMargin

      assert(help == expected)
    }
    test("hidden commands in help message") {
      val entryPoint = new CommandsEntryPoint {
        def progName                = "foo"
        override def defaultCommand = Some(HiddenCommands.First)
        def commands = Seq(HiddenCommands.First, HiddenCommands.Second, HiddenCommands.Third)
      }
      test("hidden by default") {
        val help = entryPoint.help.help(format)
        val expected =
          """Usage: foo <COMMAND> [options]
            |
            |Help options:
            |  --usage            Print usage and exit
            |  -h, -help, --help  Print help message and exit
            |
            |Other options:
            |  -f, --foo string
            |  --bar int
            |
            |Aa commands:
            |  third  Third help message
            |
            |Bb commands:
            |  second""".stripMargin

        assert(help == expected)
      }
      test("shown when asked") {
        val help = entryPoint.help.help(format, showHidden = true)
        val expected =
          """Usage: foo <COMMAND> [options]
            |
            |Help options:
            |  --usage            Print usage and exit
            |  -h, -help, --help  Print help message and exit
            |
            |Other options:
            |  -f, --foo string
            |  --bar int
            |
            |Aa commands:
            |  first  (hidden)
            |  third  Third help message
            |
            |Bb commands:
            |  second""".stripMargin

        assert(help == expected)
      }
    }

  }

}

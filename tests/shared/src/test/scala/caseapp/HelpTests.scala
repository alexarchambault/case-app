package caseapp

import caseapp.core.help.Help
import utest._

object HelpTests extends TestSuite {

  import Definitions._

  case class Options(
    first: Int,
    @Hidden
      second: String
  )

  case class WithValueDescription(
    @ValueDescription("overridden description") value: String
  )

  val tests = TestSuite {

    // https://github.com/scala/bug/issues/11125#issuecomment-423375868
    def lines(s: String) = augmentString(s).lines.toVector
    def checkLines(message: String, expectedMessage: String) = {
      val messageLines = lines(message)
      val expectedLines = lines(expectedMessage)

      for (((a, b), idx) <- messageLines.zip(expectedLines).zipWithIndex if a != b)
        Console.err.println(s"Line $idx, expected '$b', got '$a'")

      assert(messageLines == expectedLines)
    }

    "generate a help message" - {

      val message = CaseApp.helpMessage[Example]

      val expectedMessage =
        """Example
          |Usage: example [options]
          |  --foo  <string>
          |  --bar  <int>""".stripMargin

      checkLines(message, expectedMessage)
    }

    "mark optional options with ? in help messages" - {
      val message = CaseApp.helpMessage[OptBool]

      val expectedMessage =
        """OptBool
          |Usage: opt-bool [options]
          |  --opt  <bool?>""".stripMargin

      checkLines(message, expectedMessage)
    }

    "mark repeatable options with * in help messages" - {
      val message = CaseApp.helpMessage[WithList]

      val expectedMessage =
        """WithList
          |Usage: with-list [options]
          |  --list  <int*>""".stripMargin

      checkLines(message, expectedMessage)
    }

    "use custom arg parser descriptions in help messages" - {
      val message = CaseApp.helpMessage[WithCustom]

      val expectedMessage =
        """WithCustom
          |Usage: with-custom [options]
          |  --custom  <custom parameter>""".stripMargin

      checkLines(message, expectedMessage)
    }

    "use value descriptions from annotations when given" - {
      val message = CaseApp.helpMessage[WithValueDescription]

      val expectedMessage =
        """WithValueDescription
          |Usage: with-value-description [options]
          |  --value  <overridden description>""".stripMargin

      checkLines(message, expectedMessage)
    }

    "don't add a help message for fields annotated with @Hidden" - {

      // https://github.com/scala/bug/issues/11125#issuecomment-423375868
      val helpLines = augmentString(Help[Options].help).lines.toVector

      * - {
        val res = helpLines.count(_.contains("--first"))
        val expectedRes = 1
        assert(res == expectedRes)
      }
      * - {
        val res = helpLines.count(_.contains("--second"))
        val expectedRes = 0
        assert(res == expectedRes)
      }
    }

    "duplicates" - {

      import Definitions.Duplicates._

      * - {
        val dup = Help[Foo].duplicates
        assert(dup.nonEmpty)

        val expected = Map("--foo-bar" -> Seq("fooBar", "foo-bar"))
        val res = dup.mapValues(_.map(_.name.name)).iterator.toMap
        assert(res == expected)
      }

      * - {
        val dup = Help[Bar].duplicates
        assert(dup.nonEmpty)

        val expected = Map("--foo-bar" -> Seq("fooBar", "other"))
        val res = dup.mapValues(_.map(_.name.name)).iterator.toMap
        assert(res == expected)
      }

      * - {
        val dup = Help[Second].duplicates
        assert(dup.nonEmpty)

        val expected = Map("--foo-bar" -> Seq("fooBar", "foo-bar"))
        val res = dup.mapValues(_.map(_.name.name)).iterator.toMap
        assert(res == expected)
      }

    }

  }

}

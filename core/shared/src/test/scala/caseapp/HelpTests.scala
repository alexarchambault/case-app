package caseapp

import java.time.Instant
import scala.util.Try
import caseapp.core.{ArgHint, ArgParser, Messages}
import org.scalatest._

class HelpTests extends FlatSpec with Matchers {

  import Definitions._

  "case-app" should "generate a help message" in {

    val message = CaseApp.helpMessage[Example]

    val expectedMessage =
      """Example
        |Usage: example [options]
        |  --foo  <string>  <default: [""]>
        |  --bar  <int>  <default: [0]>""".stripMargin
    check(message, expectedMessage)
  }

  it should "not add a help message for fields annotated with @Hidden" in {
    case class Options(
      first: Int,
      @Hidden
      second: String
    )

    val helpLines = Messages[Options].helpMessage.lines.toVector

    helpLines.count(_.contains("--first")) shouldBe 1
    helpLines.count(_.contains("--second")) shouldBe 0
  }

  it should "render * for repeated args" in {
    val message = CaseApp.helpMessage[WithList]
    val expected =
      """WithList
        |Usage: with-list [options]
        |  --list  <int*>  <default: [List()]>""".stripMargin
    check(message, expected)
  }


  it should "render required/optional state of args" in {
    val message = CaseApp.helpMessage[OptBool]
    val expected =
      """OptBool
        |Usage: opt-bool [options]
        |  --opt  <bool?>  <default: [None]>""".stripMargin
    check(message, expected)
  }

  it should "render default args' values" in {
    val message = CaseApp.helpMessage[MoreArgs]
    val expected =
      """MoreArgs
        |Usage: more-args [options]
        |  --count  <default: [0]>
        |  --value  <string>  <default: ["default"]>
        |  --num-foo  <int>  <default: [-10]>""".stripMargin
    check(message, expected)
  }

  it should "render 'required' when no Default instance is defined" in {
    val message = CaseApp.helpMessage[ReqOpt]
    val expected =
      """ReqOpt
        |Usage: req-opt [options]
        |  --no-default  <has-no-default-value (no sense either)>  <required>""".stripMargin
    check(message, expected)
  }

  private def check(message: String, expectedMessage: String) = {
    def lines(s: String) = s.lines.toVector

    val diff = (lines(message) zip lines(expectedMessage)).filter {
      case (a, b) =>
        a != b
    }
    if (diff.nonEmpty) {
      println(diff)
    }

    lines(message) shouldBe lines(expectedMessage)
  }
}

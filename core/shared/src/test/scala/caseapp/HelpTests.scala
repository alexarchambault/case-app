package caseapp

import caseapp.core.Messages

import org.scalatest._

class HelpTests extends FlatSpec with Matchers {

  import Definitions._

  "case-app" should "generate a help message" in {

    val message = CaseApp.helpMessage[Example]

    val expectedMessage =
      """Example
        |Usage: example [options]
        |  --foo  <string>  [required]
        |  --bar  <int>  [required]""".stripMargin
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
        |  --list  <int*>  [required]""".stripMargin
    check(message, expected)
  }


  it should "render required/optional state of args" in {
    val message = CaseApp.helpMessage[OptBool]
    val expected =
      """OptBool
        |Usage: opt-bool [options]
        |  --opt  <bool?>  [required]""".stripMargin
    check(message, expected)
  }

  it should "render default args' values" in {
    val message = CaseApp.helpMessage[MoreArgs]
    val expected =
      """MoreArgs
        |Usage: more-args [options]
        |  --count  [required]
        |  --value  <string>  [required]  [default: default]
        |  --num-foo  <int>  [required]  [default: -10]""".stripMargin
    check(message, expected)
  }

  private def check(message: String, expectedMessage: String) = {
    def lines(s: String) = s.lines.toVector

    println((lines(message) zip lines(expectedMessage)).filter {
      case (a, b) =>
        a != b
    })

    lines(message) shouldBe lines(expectedMessage)
  }
}

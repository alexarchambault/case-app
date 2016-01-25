package caseapp

import caseapp.core.Messages
import org.scalatest._

class HelpTests extends FlatSpec with Matchers {

  "case-app" should "not add a help message for fields annotated with @Hidden" in {
    case class Options(
      first: Int,
      @Hidden
      second: String
    )

    val helpLines = Messages[Options].helpMessage.linesIterator.toVector

    helpLines.count(_.contains("--first")) shouldBe 1
    helpLines.count(_.contains("--second")) shouldBe 0
  }

}

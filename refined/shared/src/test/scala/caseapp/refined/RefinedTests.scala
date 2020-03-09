package caseapp.refined

import caseapp.Name
import caseapp.core.Error
import caseapp.core.parser.Parser
import eu.timepit.refined.refineMV
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import utest._
import caseapp.core.util.OptionFormatter

object RefinedTests extends TestSuite {

  val tests = Tests {

    "simple" - {
      case class Args(n: Int, pos: Refined[Int, Positive])
      val res = Parser[Args].parse(Seq("-n", "-6", "--pos", "3"))
      val expectedRes = Right((Args(-6, refineMV(3)), Seq.empty))
      assert(res == expectedRes)
    }

    "error" - {
      case class Args(n: Int, pos: Refined[Int, Positive] = refineMV(1))
      val res = Parser[Args].parse(Seq("-n", "-6", "--pos", "-3"))
      val expectedRes = Left(
        // wish refined didn't add parentheses around the error
        Error.ParsingArgument(Name("pos"), Error.Other("(-3 > 0)"), OptionFormatter.DefaultFormatter)
      )
      assert(res == expectedRes)
    }

  }

}

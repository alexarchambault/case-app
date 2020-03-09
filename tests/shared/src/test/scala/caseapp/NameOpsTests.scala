package caseapp

import caseapp.core.Error
import caseapp.core.Error.SeveralErrors
import caseapp.core.help.{Help, WithHelp}
import caseapp.demo._
import shapeless.{Inl, Inr}
import utest._
import caseapp.core.util.OptionFormatter

object NameOpsTests extends TestSuite {

  final case class FewArgs(
      numBar: Int = -10,
      value: String = "default",
      numFoo: Int = -10
  )

  val tests = TestSuite {

    "parse" - {

      val res =
        Parser[FewArgs]
          .optionFormatter((n: Name) => n.name)
          .detailedParse(
            Seq("--value", "b", "--numFoo", "1", "--numBar", "2")
          )

      val expectedRes =
        Right(
          (FewArgs(2, "b", 1), RemainingArgs(Seq(), Seq()))
        )
      assert(res == expectedRes)
    }

  }

}

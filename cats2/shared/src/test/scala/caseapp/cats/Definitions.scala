package caseapp
package cats

import _root_.cats.data.NonEmptyList

object Definitions {

  final case class FewArgs(
    value  : String = "default",
    numFoo : Int = -10
  )

  final case class WithNonEmptyList(nel: NonEmptyList[String])

  sealed trait Command

  case class First(
    @ExtraName("f")
      foo: String = "",
    bar: Int = 0
  ) extends Command

  case class Second(
    fooh: String = "",
    baz: Int = 0
  ) extends Command

  @HelpMessage("Third help message")
  case class Third(
    third: Int = 0
  ) extends Command

  object CommandTest extends CommandApp[Command] {
    def run(options: Command, remainingArgs: RemainingArgs): Unit = {}
  }

}

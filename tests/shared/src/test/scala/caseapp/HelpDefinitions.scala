package caseapp

object HelpDefinitions {
  case class FirstOptions(
    @ExtraName("f")
      foo: String = "",
    bar: Int = 0
  )

  case class SecondOptions(
    fooh: String = "",
    baz: Int = 0
  )

  @HelpMessage("Third help message")
  case class ThirdOptions(
    third: Int = 0
  )

  object First extends Command[FirstOptions] {
    def run(options: FirstOptions, args: RemainingArgs) = ???
  }
  object Second extends Command[SecondOptions] {
    def run(options: SecondOptions, args: RemainingArgs) = ???
  }
  object Third extends Command[ThirdOptions] {
    def run(options: ThirdOptions, args: RemainingArgs) = ???
  }

  @HelpMessage("Example help message")
  final case class GroupedOptions(
    @Group("Something")
      foo: String,
    @Group("Bb")
      bar: Int,
    @Group("Something")
      other: Double,
    @Group("Bb")
      something: Boolean
  )
}

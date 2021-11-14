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

  @HelpMessage("Example help message")
  final case class HiddenGroupOptions(
    @Group("Something")
    foo: String,
    @Group("Bb")
    @Hidden
    bar: Int,
    @Group("Something")
    other: Double,
    @Group("Bb")
    @Hidden
    something: Boolean
  )

  object CommandGroups {
    object First extends Command[FirstOptions] {
      override def group                                  = "Aa"
      def run(options: FirstOptions, args: RemainingArgs) = ???
    }
    object Second extends Command[SecondOptions] {
      override def group                                   = "Bb"
      def run(options: SecondOptions, args: RemainingArgs) = ???
    }
    object Third extends Command[ThirdOptions] {
      override def group                                  = "Aa"
      def run(options: ThirdOptions, args: RemainingArgs) = ???
    }
  }

  object HiddenCommands {
    object First extends Command[FirstOptions] {
      override def group                                  = "Aa"
      override def hidden                                 = true
      def run(options: FirstOptions, args: RemainingArgs) = ???
    }
    object Second extends Command[SecondOptions] {
      override def group                                   = "Bb"
      def run(options: SecondOptions, args: RemainingArgs) = ???
    }
    object Third extends Command[ThirdOptions] {
      override def group                                  = "Aa"
      def run(options: ThirdOptions, args: RemainingArgs) = ???
    }
  }
}

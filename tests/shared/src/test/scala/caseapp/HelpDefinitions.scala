package caseapp

object HelpDefinitions {
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

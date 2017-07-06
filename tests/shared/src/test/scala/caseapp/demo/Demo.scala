package caseapp
package demo

@AppVersion("0.1.0")
@ArgsName("files")
final case class DemoOptions(
  first: Boolean,
  @ExtraName("V") @HelpMessage("Set a value") value : Option[String],
  @ExtraName("v") @HelpMessage("Be verbose") verbose : Int @@ Counter,
  @ExtraName("S") @ValueDescription("stages")  stages : List[String]
)

object Demo extends CaseApp[DemoOptions] {

  def run(options: DemoOptions, remainingArgs: RemainingArgs) =
    Console.err.println(this)
}

@AppName("Glorious App")
@AppVersion("0.1.0")
final case class MyAppOptions(
  @ValueDescription("a foo") @HelpMessage("Specify some foo") foo: Option[String],
  bar: Int
)

object MyApp extends CaseApp[MyAppOptions] {
  def run(options: MyAppOptions, remainingArgs: RemainingArgs) = {}
}


@AppName("Demo")
@AppVersion("1.0.0")
@ProgName("demo-cli")
sealed abstract class DemoCommand extends Product with Serializable

final case class First(
  @ExtraName("v") verbose: Int @@ Counter,
  @ValueDescription("a bar") @HelpMessage("Set bar") bar: String = "default-bar"
) extends DemoCommand

@CommandName("second")
final case class Secondd(
  extra: List[Int],
  @ExtraName("S")
  @ValueDescription("stages")
    stages : List[String]
) extends DemoCommand

object CommandAppTest extends CommandApp[DemoCommand] {

  def run(options: DemoCommand, remainingArgs: RemainingArgs): Unit =
    options match {
      case _: First =>
        Console.err.println(s"First: $this")
      case _: Secondd =>
        Console.err.println(s"Second: $this")
    }
}
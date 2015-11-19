package caseapp
package demo

@AppVersion("0.1.0")
@ArgsName("files")
case class Demo(
  first: Boolean
, @ExtraName("V") @HelpMessage("Set a value") value : Option[String]
, @ExtraName("v") @HelpMessage("Be verbose") verbose : Int @@ Counter
, @ExtraName("S") @ValueDescription("stages")  stages : List[String]
) extends App {

  Console.err.println(this)

}

@AppName("Glorious App")
@AppVersion("0.1.0")
case class MyApp(
  @ValueDescription("a foo") @HelpMessage("Specify some foo") foo: Option[String]
, bar: Int
) extends App {
// ...
}

object MyApp0 extends AppOf[MyApp]

object DemoApp extends AppOf[Demo]


@AppName("Demo")
@AppVersion("1.0.0")
@ProgName("demo-cli")
sealed trait DemoCommand extends Command

case class First(
  @ExtraName("v") verbose: Int @@ Counter,
  @ValueDescription("a bar") @HelpMessage("Set bar") bar: String = "default-bar"
) extends DemoCommand {

  Console.err.println(s"First: $this")

}

@CommandName("second")
case class Secondd(
  extra: List[Int],
  @ExtraName("S")
  @ValueDescription("stages")
    stages : List[String]
) extends DemoCommand {

  Console.err.println(s"Second: $this")

}

object CommandApp extends CommandAppOf[DemoCommand]
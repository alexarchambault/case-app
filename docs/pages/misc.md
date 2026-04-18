# Miscellaneous

## cats-effect

```scala mdoc:reset-object:invisible
import caseapp._
```

case-app has a module helping using it in cats-effect applications.

Add a dependency to it like

```scala mdoc:passthrough
println("```scala")
println("//> using dep com.github.alexarchambault::case-app-cats:@VERSION@")
println("```")
```
(for other build tools, see [setup](setup.md) and change `case-app` to `case-app-cats`)

### Single command

Use `IOCaseApp` for a single-command application:
```scala mdoc:silent
import caseapp.catseffect._
import cats.data.NonEmptyList
import cats.effect._

case class ExampleOptions(
  foo: String = "",
  thing: NonEmptyList[String]
)

object IOCaseExample extends IOCaseApp[ExampleOptions] {
  def run(options: ExampleOptions, arg: RemainingArgs): IO[ExitCode] = IO {
    // Core of the app
    // ...
    ExitCode.Success
  }
}
```

### Multiple subcommands

Use `IOCommand` and `IOCommandsEntryPoint` for applications with
multiple subcommands — the cats-effect equivalents of `Command` and
`CommandsEntryPoint`:

```scala mdoc:reset-object:silent
import caseapp._
import caseapp.catseffect._
import cats.effect._

case class GrindOptions(
  @HelpMessage("Type of beans")
  beans: String = "arabica",
  @HelpMessage("Grind size")
  size: String = "medium"
)

object Grind extends IOCommand[GrindOptions] {
  override def names = List(List("grind"))
  def run(options: GrindOptions, args: RemainingArgs): IO[ExitCode] =
    IO.println(s"Grinding ${options.beans} (${options.size})").as(ExitCode.Success)
}

case class BrewOptions(
  @HelpMessage("Brewing method")
  method: String = "pourover",
  @HelpMessage("Water temperature in Celsius")
  temp: Int = 96
)

object Brew extends IOCommand[BrewOptions] {
  override def names = List(List("brew"))
  def run(options: BrewOptions, args: RemainingArgs): IO[ExitCode] =
    IO.println(s"Brewing with ${options.method} at ${options.temp}°C").as(ExitCode.Success)
}

object CoffeeApp extends IOCommandsEntryPoint {
  def progName = "coffee"
  def commands = Seq(Grind, Brew)
}
```

`IOCommand` supports the same features as `Command`: custom `names`,
`group`, `hidden`, and tab completion via `completer`.

`IOCommandsEntryPoint` supports `defaultCommand` for a fallback
when no subcommand is specified, just like `CommandsEntryPoint`.

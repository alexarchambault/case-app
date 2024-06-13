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

Then use it like
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

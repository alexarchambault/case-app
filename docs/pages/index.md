# case-app

*case-app* is a command-line argument parser for Scala that relies on case classes.

## Quick start

Add case-app to your project (see [setup](setup.md) for Mill, Scala.js, Scala Native, and snapshots):
```scala
//> using dep com.github.alexarchambault::case-app:@VERSION@
```
```scala
// sbt
libraryDependencies += "com.github.alexarchambault" %% "case-app" % "@VERSION@"
```
```scala
// Mill
def mvnDeps = Seq(mvn"com.github.alexarchambault::case-app:@VERSION@")
```

Define your options in a case class, and your application as an object extending `CaseApp`:

```scala mdoc:silent
import caseapp._

@ProgName("my-app")
case class Options(
  @HelpMessage("Name of the person to greet")
  @Name("n")
    name: String = "world",
  @HelpMessage("Number of greetings")
    count: Int = 1
)

object MyApp extends CaseApp[Options] {
  def run(options: Options, args: RemainingArgs): Unit =
    for (_ <- 1 to options.count)
      println(s"Hello ${options.name}!")
}
```

Running `my-app -n Alex --count 2` prints `Hello Alex!` twice, and `my-app --help` prints
```scala mdoc:passthrough
// FIXME Find a way to keep colors in the output
import caseapp.core.help.HelpFormat
println("```text")
println(MyApp.finalHelp.help(HelpFormat.default(ansiColors = false), showHidden = false))
println("```")
```

## Features

- options defined as fields of [case classes](define.md), with [many supported types](types.md)
  and default values
- [sub-commands](commands.md)
- [help message](help.md) generation, customizable via annotations
- [shell completions](completion.md) (bash, zsh, fish)
- a [cats-effect](misc.md#cats-effect) module
- runs on the JVM, [Scala.js](https://github.com/scala-js/scala-js), and
  [Scala Native](https://github.com/scala-native/scala-native)

case-app is published for Scala 2.13 and Scala 3 on the JVM, and for Scala 3 on
Scala.js and Scala Native (the cats-effect module is only published for Scala 3).

## Used by

case-app is used by [coursier](https://github.com/coursier/coursier),
[Scala CLI](https://github.com/VirtusLab/scala-cli),
[Almond](https://github.com/almond-sh/almond), …

## License

case-app is released under the
[Apache 2.0 license](https://github.com/alexarchambault/case-app/blob/main/LICENSE).

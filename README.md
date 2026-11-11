# case-app

*Type-level & seamless command-line argument parsing for Scala*

[![Build status](https://github.com/alexarchambault/case-app/workflows/CI/badge.svg)](https://github.com/alexarchambault/case-app/actions?query=workflow%3ACI)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.alexarchambault/case-app_3.svg)](https://central.sonatype.com/artifact/com.github.alexarchambault/case-app_3)
[![javadoc](https://javadoc.io/badge2/com.github.alexarchambault/case-app_3/javadoc.svg)](https://javadoc.io/doc/com.github.alexarchambault/case-app_3)

case-app is a command-line argument parser for Scala. You define options as the fields of a case class,
and case-app parses arguments into it and generates help messages.

## Quick start

Add case-app to your project. The latest version is shown in the Maven Central badge above.
See the [setup page](https://alexarchambault.github.io/case-app/setup/) for Scala.js, Scala Native,
and more details.
```scala
// Scala CLI
//> using dep com.github.alexarchambault::case-app:2.2.0
// sbt
libraryDependencies += "com.github.alexarchambault" %% "case-app" % "2.2.0"
// Mill
def mvnDeps = Seq(mvn"com.github.alexarchambault::case-app:2.2.0")
```

Define your options in a case class, and your application as an object extending `CaseApp`:
```scala
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
```text
Usage: my-app [options]

Help options:
  --usage            Print usage and exit
  -h, -help, --help  Print help message and exit

Other options:
  -n, --name string  Name of the person to greet
  --count int        Number of greetings
```

## Features

- options defined as fields of case classes, with many supported types and default values
- sub-commands
- help message generation, customizable via annotations
- shell completions (bash, zsh, fish)
- a cats-effect module
- runs on the JVM, [Scala.js](https://github.com/scala-js/scala-js), and
  [Scala Native](https://github.com/scala-native/scala-native)

case-app is published for Scala 2.12, 2.13, and 3 on the JVM, and for Scala 3 on
Scala.js and Scala Native (the cats-effect module is only published for Scala 3).

## Documentation

See the [website](https://alexarchambault.github.io/case-app/) for more details, in particular:
- [defining options](https://alexarchambault.github.io/case-app/define/)
- [supported types](https://alexarchambault.github.io/case-app/types/)
- [parsing arguments](https://alexarchambault.github.io/case-app/parse/)
- [customizing help](https://alexarchambault.github.io/case-app/help/)
- [commands](https://alexarchambault.github.io/case-app/commands/)
- [completion](https://alexarchambault.github.io/case-app/completion/)

## Used by

case-app is used by [coursier](https://github.com/coursier/coursier),
[Scala CLI](https://github.com/VirtusLab/scala-cli),
[Almond](https://github.com/almond-sh/almond), …

## License

case-app is released under the [Apache 2.0 license](LICENSE).

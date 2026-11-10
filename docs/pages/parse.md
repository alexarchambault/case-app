# Parsing options

case-app offers several ways to parse input arguments:

- method calls,
- app definitions.

## Method calls

The `CaseApp` object contains a number of methods that can be used to parse arguments.

### `parse`

```scala mdoc:invisible:reset
import caseapp._
```

`CaseApp.parse` accepts a sequence of strings, and returns either an error or
parsed options and remaining arguments:
```scala mdoc:silent
case class Options(
  foo: Int = 0
)
val args = Seq("a", "--foo", "2", "b")
val (options, remaining) = CaseApp.parse[Options](args).toOption.get
assert(options == Options(2))
assert(remaining == Seq("a", "b"))

val either = CaseApp.parse[Options](Seq("--foo", "a"))
assert(either.left.toOption.nonEmpty)
```

### `parseWithHelp`

`CaseApp.parseWithHelp` does the same as `CaseApp.parse`, but also accepts
`--help` / `-h` / `--usage` options.

```scala mdoc:silent
CaseApp.parseWithHelp[Options](args) match {
  case Left(error) => // invalid options…
  case Right((Left(error), helpAsked, usageAsked, remaining)) =>
    // missing mandatory options, but --help or --usage could be parsed
  case Right((Right(options), helpAsked, usageAsked, remaining)) =>
    // All is well:
    // Options were parsed, resulting in options
    // helpAsked and / or usageAsked are true if either has been requested
    // remaining contains non-option arguments
}
```

### `detailedParse`, `detailedParseWithHelp`

`CaseApp.detailedParse` and `CaseApp.detailedParseWithHelp` behave the same way
as `CaseApp.parse` and `CaseApp.parseWithHelp`, but return their remaining arguments
as a `RemainingArgs` instance, rather than a `Seq[String]`. See [below](#double-hyphen)
for what `RemainingArgs` brings.

### `process`

`CaseApp.process` is the most straightforward method to parse arguments. Note that
it exits the current application if parsing arguments fails or if users request
help, with `--help` for example.

```scala mdoc:reset:invisible
val args = Array("a")
```

It aims at being used from Scala CLI `.sc` files ("Scala scripts"), where one
would rather have case-app handle all error cases, like
```scala mdoc:silent
//> using dep com.github.alexarchambault::case-app::@VERSION@
import caseapp._

case class Options(
  foo: Int = 0,
  path: Option[String] = None
)

val (options, remaining) = CaseApp.process[Options](args.toSeq)

// …
```

## Application definition

case-app allows one to alter one's main class definitions, so that one
defines a method accepting parsed options, rather than raw arguments:

```scala mdoc:reset-object:invisible
import caseapp._
```

```scala mdoc:silent
case class Options(
  foo: Int = 0
)

object MyApp extends CaseApp[Options] {
  def run(options: Options, remaining: RemainingArgs): Unit = {
    ???
  }
}
```

In that example, `CaseApp` defines the `main` method, so that
`MyApp` can be used as a "main class".

### Errors, help, and exit codes

When its `main` method is called, `CaseApp`:

- calls `run` if arguments are parsed successfully,
- calls `error` if parsing fails (malformed value, missing mandatory option,
  unrecognized option, etc.), which prints the error message on the standard error
  and exits with code 1,
- calls `helpAsked`, `usageAsked`, or `fullHelpAsked` if users passed `--help` / `-h`,
  `--usage`, or `--full-help` (see [hidden options](help.md#hidden-options)), which print
  the corresponding message on the standard output and exit with code 0.

All these methods can be overridden. They rely on `exit`, that exits the application
with `sys.exit` (or `process.exit` on Scala.js), and `printLine`, that prints a line
on the standard output or error. Override `exit` and `printLine` to change how the
application exits or where messages are printed, or override `error`, `helpAsked`,
etc. to change what is done in each case.

```scala mdoc:reset-object:invisible
import caseapp._
```

For example, in order to test an application without exiting the JVM, and to exit
with a custom message and exit code on errors, one can do:
```scala mdoc:silent
import caseapp.core.Error

final class ExitException(val code: Int) extends Exception

case class Options(
  count: Int
)

object MyApp extends CaseApp[Options] {
  override def exit(code: Int): Nothing =
    throw new ExitException(code)
  override def error(message: Error): Nothing = {
    printLine(s"Error: ${message.message}", toStderr = true)
    printLine("Run my-app --help for more details", toStderr = true)
    exit(2)
  }
  override def printLine(line: String, toStderr: Boolean): Unit =
    println(if (toStderr) s"[stderr] $line" else line)
  def run(options: Options, args: RemainingArgs): Unit =
    printLine(s"Count: ${options.count}")
}

def runApp(args: String*): Int =
  try {
    MyApp.main(args.toArray)
    0
  }
  catch {
    case e: ExitException => e.code
  }
```

```scala mdoc
runApp("--count", "2")
runApp("--count", "a")
runApp("--count", "2", "--other")
runApp()
```

Applications made of [commands](commands.md) behave the same way: once a command is
found, parsing its arguments and handling errors is done by the `CaseApp` methods of
that command. If no command matches the arguments, and no default command is
defined, `CommandsEntryPoint` prints its help message and exits with code 0. Like
`CaseApp`, `CommandsEntryPoint` has `exit` and `printLine` methods that can
be overridden.

## Parsing

### Double-hyphen

```scala mdoc:invisible:reset
import caseapp._
```

case-app assumes any argument after `--` is not an option. It stops looking
for options after `--`. Arguments before and after `--` can be differentiated
in the `RemainingArgs` class:
```scala mdoc:silent
case class Options()
val (_, args) = CaseApp.detailedParse[Options](Seq("first", "--", "foo")).toOption.get
```
```scala mdoc
args.remaining
args.unparsed
args.all
```

# Advanced topics

## Recovering argument positions

```scala mdoc:reset:invisible
import caseapp._
```

One can recover the exact positions of each options and remaining arguments.

For options, one should use the `Indexed` class, like
```scala mdoc
import caseapp.core.Indexed

case class Options(
  foo: Indexed[String] = Indexed("")
)

val (options, _) = CaseApp.parse[Options](Seq("a", "--foo", "thing")).toOption.get
```

For arguments that are not options or option values, indices are retained in the
`RemainingArgs` instance:
```scala mdoc:silent
val (_, args) = CaseApp.detailedParse[Options](Seq("a", "--foo", "2", "b")).toOption.get
```
```scala mdoc
args.indexed
```

## Partial parsing

```scala mdoc:reset:invisible
import caseapp._
```

One can stop parsing arguments at the first argument that is not an option,
or that is an unknown option. This can be useful when "pre-processing" arguments
passed to another application later on: case-app parses as much arguments as possible,
then stops, so that the other application can parse the remaining arguments later on.

```scala mdoc:silent
case class Options(
  foo: String = ""
)

object Options {
  implicit lazy val parser: Parser[Options] = {
    val parser0: Parser[Options] = Parser.derive
    parser0.stopAtFirstUnrecognized
  }
  implicit lazy val help: Help[Options] = Help.derive
}
```

```scala mdoc
val (options, args) = CaseApp.parse[Options](Seq("--foo", "a", "--other", "thing")).toOption.get
```


```scala mdoc:reset-object:invisible
import caseapp._
```

```scala mdoc:invisible
// keep in sync with Options above
case class Options(
  foo: String = ""
)
```

Alternatively, when extending `CaseApp`, one can do:
```scala mdoc:silent
object MyApp extends CaseApp[Options] {
  override def stopAtFirstUnrecognized = true
  def run(options: Options, args: RemainingArgs): Unit = {
    ???
  }
}
```

```scala mdoc:invisible
val (_, args0) = MyApp.parser.parse(Seq("--foo", "a", "--other", "thing")).toOption.get
assert(args0 == Seq("--other", "thing"))
```

## Ignore unrecognized options

```scala mdoc:reset-object:invisible
import caseapp._
```

One can ignore non-recognized options, like
```scala mdoc:silent
case class Options(
  foo: String = ""
)

object Options {
  implicit lazy val parser: Parser[Options] = {
    val parser0: Parser[Options] = Parser.derive
    parser0.ignoreUnrecognized
  }
  implicit lazy val help: Help[Options] = Help.derive
}
```

```scala mdoc
val (options, args) = CaseApp.parse[Options](Seq("--other", "thing", "--foo", "a")).toOption.get
```

Unrecognized options end up in the "remaining arguments", along with non-option or non-option
values arguments.

```scala mdoc:reset-object:invisible
import caseapp._
```

```scala mdoc:invisible
// keep in sync with Options above
case class Options(
  foo: String = ""
)
```

Alternatively, when extending `CaseApp`, one can do:
```scala mdoc:silent
object MyApp extends CaseApp[Options] {
  override def ignoreUnrecognized = true
  def run(options: Options, args: RemainingArgs): Unit = {
    ???
  }
}
```

```scala mdoc:invisible
val (_, args0) = MyApp.parser.parse(Seq("--other", "thing", "--foo", "a")).toOption.get
assert(args0 == Seq("--other", "thing"))
```

## Check for duplicate options in tests

```scala mdoc:reset-object:invisible
import caseapp._
```

When using the `@Recurse` or `@Name` annotations, some options might be given the same
name. In practice, at runtime, one will shadow the other.

To ensure your options don't contain duplicates, you can call `ensureNoDuplicates`
on the `Help` type class instance of your options, or on the `CaseApp` instance
if you defined one:
```scala mdoc:silent
case class Options(
  foo: String = "",
  @Name("foo")
    count: Int = 0
)

object MyApp extends CaseApp[Options] {
  def run(options: Options, args: RemainingArgs): Unit = {
    ???
  }
}
```

```scala mdoc:crash
Help[Options].ensureNoDuplicates()
```

```scala mdoc:crash
MyApp.ensureNoDuplicates()
```

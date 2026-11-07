# Option types

case-app pre-defines parsers for a number of standard types, and allows you
to define parsers of your own.

## Booleans

Boolean fields are mapped to "flags", that is options not requiring a value:
```scala mdoc:reset:silent
case class Options(
  foo: Boolean // --foo
)
```

While these do not require a value, one can be passed explicitly, with an `=` sign,
like `--foo=true` or `--foo=false`. This is especially useful for boolean fields
whose default value is `true` and is not changed when users specify the flag without a
value.

## Strings

```scala mdoc:reset:invisible
import caseapp._
```

String fields are given the value passed to the option as is:
```scala mdoc
case class Options(
  foo: String
)

val (options, _) = CaseApp.parse[Options](Seq("--foo", "123")).toOption.get
```

## Numerical values

Integer types like `Int`, `Long`, `Short`, `Byte`, and floating ones like
`Double`, `Float`, `BigDecimal` are all accepted as field types.

## Options

Field types wrapped in `Option[_]` are automatically non-mandatory, even
when the field doesn't have a default value.

Option types are convenient to know whether an option was specified (the
field value is then `Some(…)`) or not (field value is `None`),
and to behave differently if the option wasn't specified, like defaulting to the value
of another option.

```scala mdoc:reset:invisible
import caseapp._
```

```scala mdoc
case class Options(
  foo: Option[Int],
  path: Option[String] = None
)

CaseApp.parse[Options](Seq("--foo", "2"))
```

Like for non-`Option` fields, specifying an `Option` option more than once
is an error:
```scala mdoc
CaseApp.parse[Options](Seq("--foo", "2", "--foo", "3"))
```
Use [`Last`](#last) or [sequences](#sequences) to accept repeated options.

## Sequences

```scala mdoc:reset:invisible
import caseapp._
```

Sequences allow users to specify an argument more than once, and get the different values
passed each time:
```scala mdoc
case class Options(
  path: List[String]
)

val (options, _) = CaseApp.parse[Options](Seq("--path", "/a", "--path", "/b", "--path", "/c")).toOption.get
```

Only `List` and `Vector` are accepted with the default parsers, no generic `Seq` for example.

The options corresponding to sequence fields are not mandatory even if the field
doesn't have a default value. If no option for it is specified, it will default to an
empty sequence.

## Last

`Last` is an ad-hoc type defined by case-app. Like sequence types, it allows an option
to be specified multiple times.

Yet, unlike sequence types, it just discards the values passed to the option, but for the
last one.

So `Last` makes option parsing not fail if its option is specified multiple times, and just
retains the last occurrence of it.

```scala mdoc:reset:invisible
import caseapp._
```

```scala mdoc
case class Options(
  mode: Last[String] = Last("default"),
  level: Option[Last[Int]] = None
)

val (options, _) = CaseApp.parse[Options](
  Seq("--mode", "fast", "--level", "1", "--mode", "slow", "--level", "3")
).toOption.get
options.mode.value
options.level.map(_.value)
```

As shown above, `Last` can be wrapped in `Option`, to know whether the option
was specified at all.

## Counters

```scala mdoc:reset:invisible
import caseapp._
```

One may want to allow flags to be specified multiple times. This can be achieved in two
ways out-of-the-box with case-app:
```scala mdoc
case class Options(
  verbose: Int @@ Counter = Tag.of(0),
  debug: List[Unit]
)

val (options, _) = CaseApp.parse[Options](
  Seq("--verbose", "--debug", "--verbose", "--verbose", "--debug")
).toOption.get
Tag.unwrap(options.verbose) // --verbose specified 3 times
options.debug.length // --debug specified 2 times
```

`@@` and `Tag` are ad-hoc types defined by case-app, that look like types with similar
names defined in the [shapeless](https://github.com/milessabin/shapeless) library. These aim
at helping "tagging" or "annotating" a type.
`Counter` is also defined in case-app.

Using `List[Unit]` as a type also works. `Unit` itself could be used as a type, but would
be of little help. A field with type `Unit` is mapped to a flag option, like
[`Boolean`](#booleans), but the `Unit` type itself doesn't allow retaining whether the flag
was specified by users or not. In a `List` on the other hand, counting the number
of `()` (`Unit` instance) in the list tells how many times the flag was specified.

## JVM-only types

```scala mdoc:reset:invisible
import caseapp._
```

On the JVM, case-app also pre-defines parsers for:

- `java.nio.file.Path`, whose values are converted with `java.nio.file.Paths.get`,
- `java.util.Calendar`, whose values must follow the `yyyy-MM-dd` format (dates
  are parsed in the default time zone of the JVM).

```scala mdoc:silent
import java.nio.file.Path
import java.util.Calendar

case class Options(
  output: Path,
  since: Calendar
)

val (options, _) = CaseApp.parse[Options](
  Seq("--output", "out/result.txt", "--since", "2024-03-01")
).toOption.get
```

```scala mdoc
options.output
options.since.get(Calendar.YEAR)
options.since.get(Calendar.MONTH) + 1
options.since.get(Calendar.DAY_OF_MONTH)
```

These parsers are not available on Scala.js and Scala Native, where you need
to [define your own parsers](#custom-parsers) for such types if needed.

## Custom parsers

```scala mdoc:reset:invisible
import caseapp._
```

For a type to be accepted as an option, it needs to have an implicit `ArgParser` in scope.

The abstract and overridable methods of `ArgParser` are quite low-level, but allow
implementing all the kinds of parsers defined in case-app.

For simple types, when one only wants to parse a string to a given type, `SimpleArgParser`
allows defining an `ArgParser` out of a `String => T` method. For example, one can
define a parser for integer values with:
```scala mdoc:silent
import caseapp.core.argparser.{ArgParser, SimpleArgParser}
import caseapp.core.Error

case class MyInt(value: Int)

implicit lazy val myIntParser: ArgParser[MyInt] =
  SimpleArgParser.from("my-int") { input =>
    try Right(MyInt(input.toInt))
    catch {
      case _: NumberFormatException =>
        Left(Error.MalformedValue("integer", input))
    }
  }
```

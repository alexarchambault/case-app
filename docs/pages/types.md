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
and behave differently if the option wasn't specified, like default to the value
of another option say.

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

So `Last` makes option parsing not fail if its option is specified multiple time, and just
retains the last occurrence of it.

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
[`Boolean`](#booleans), but the `Unit` type itself doesn't allow to retain if the flag
was specified by users or not. In a `List` on the other hand, counting the number
of `()` (`Unit` instance) in the list allows to know how many types the flag was specified.

## Custom parsers

```scala mdoc:reset:invisible
import caseapp._
```

For a type to be accepted as an option, it needs to have an implicit `ArgParser` in scope.

The abstract and overriddable methods of `ArgParser` are quite low-level, but allow to
implement all the kind of parsers defined in case-app.

For simple types, when one only wants to parse a string to a given type, `SimpleArgParser`
allows to define an `ArgParser` out of a `String => T` method. For example, one can
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

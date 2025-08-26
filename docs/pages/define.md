# Defining options


## Case classes

Options are defined in case classes, like
```scala mdoc:silent
case class Options(
  foo: Int, // --foo 2, --foo=2
  enableThing: Boolean // --enable-thing, --enable-thing=false
)
```

## Caching derived type classes

```scala mdoc:invisible:reset
import caseapp._
```

```scala mdoc:silent
// should be the same as below, but for the Scala 3 section
case class Options(
  foo: Int
)

// Scala 2
object Options {
  implicit lazy val parser: Parser[Options] = Parser.derive
  implicit lazy val help: Help[Options] = Help.derive
}
```

When defining a case class for options, it is recommended to derive case-app
type classes for it in its companion object, like
```scala
case class Options(
  foo: Int
)

// Scala 2
object Options {
  implicit lazy val parser: Parser[Options] = Parser.derive
  implicit lazy val help: Help[Options] = Help.derive
}

// Scala 3
@derives[Parser, Help]
object Options
```

These derivations are omitted in all other examples through out the case-app
documentation for brevity, but we highly recommend deriving them there, in order
to make incremental compilation faster when the file defining the options isn't modified.
This is especially the case in Scala 2, where those type class derivation can be somewhat
slow.

## Mandatory options

```scala mdoc:invisible:reset
import caseapp._
```

Options that don't have default values are assumed to be mandatory. To
make an option non-mandatory, ensure it has a default value:
```scala mdoc:silent
case class Options(
  foo: Int, // --foo is mandatory
  verbosity: Int = 0 // --verbosity is optional
)
```

## Shared options

```scala mdoc:invisible:reset
import caseapp._
```

Options can be defined across several case classes thanks to the `@Recurse`
annotation, like
```scala mdoc:silent
case class SharedOptions(
  foo: Int = 0,
  enableThing: Boolean = false
)

case class Options(
  @Recurse
    shared: SharedOptions = SharedOptions(),
  path: String
)
```

Shared option classes can themselves have fields marked with `@Recurse`, whose
types can also have fields marked with it, etc.

## Extra names

```scala mdoc:invisible:reset
import caseapp._
```

Various annotations allow to customize options, like `@Name` to offer
several ways to specify an option:
```scala mdoc:silent
case class Options(
  @Name("f")
    foo: Int = 0, // -f 2 and --foo 2 both work
  @Name("t")
  @Name("thing")
    enableThing: Boolean = false // -t, --thing, --enable-thing all work
)
```

## Field name to option name conversion

```scala mdoc:invisible:reset
import caseapp._
```

Case class field names are assumed to follow the
[camel case](https://en.wikipedia.org/wiki/Camel_case) (`camelCase`), and are converted to
[kebab case](https://developer.mozilla.org/en-US/docs/Glossary/Kebab_case) (`kebab-case`) to
get the corresponding option name.

Names consisting in a single letter are
prefixed with a single hyphen (like `-t`) while other are prefixed with two hyphens
(like `--foo-thing`).
```scala mdoc:silent
case class Options(
  @Name("f")
    foo: Int = 0, // defines options -f and --foo
  @Name("t")
  @Name("thing")
    enableThing: Boolean = false // defines options -t, --thing, and --enable-thing
)
```

```scala mdoc:invisible:reset
import caseapp._
```

To enforce a single hyphen for longer options, put the hyphen in the field name,
and use kebab case directly, like
```scala mdoc:silent
case class Options(
  @Name("-foo")
    foo: Int = 0, // accepts both --foo and -foo
  `-thing`: Boolean = false // accepts only -thing
)
```

## Passing values to options

For option types expecting a value (most of them), that value can be passed in a separated
argument, like
```text
--foo value
```
or with an `=`, like
```text
--foo=value
```

For option types not expecting a value by default, like `Boolean`, an optional value
can be passed with an `=`, like
```text
--foo=false
```

```scala mdoc:invisible:reset
import caseapp._
```

This is especially useful for boolean options that are true by default, like
```scala mdoc:silent
case class Options(
  foo: Boolean = true // can only be disabled with --foo=false
)
```


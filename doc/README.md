# case-app

*Type-level & seamless command-line argument parsing for Scala*

[![Build Status](https://travis-ci.org/alexarchambault/case-app.svg)](https://travis-ci.org/alexarchambault/case-app)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/alexarchambault/case-app?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.alexarchambault/case-app_2.12.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.alexarchambault/case-app_2.12)
[![Scaladoc](http://javadoc-badge.appspot.com/com.github.alexarchambault/case-app_2.12.svg?label=scaladoc)](http://javadoc-badge.appspot.com/com.github.alexarchambault/case-app_2.12)

### Imports

The code snippets below assume that the content of `caseapp` is imported,

```tut:silent
import caseapp._
```

### Parse a simple set of options

```tut:silent
case class Options(
  user: Option[String],
  enableFoo: Boolean = false,
  file: List[String]
)

CaseApp.parse[Options](
  Seq("--user", "alice", "--file", "a", "--file", "b")
) == Right((Options(Some("alice"), false, List("a", "b")), Seq.empty))
```

```tut:invisible
assert(
  CaseApp.parse[Options](
    Seq("--user", "alice", "--file", "a", "--file", "b")
  ) == Right((Options(Some("alice"), false, List("a", "b")), Seq.empty))
)
```

### Required and optional arguments

All arguments are required by default. To define an optional argument simply
wrap its type into `Option[T]`.

Optional arguments can also be defined by providing a default value.
There are two ways to do that:
- providing default value ad hoc in the case class definition
- defining default value for a type with [Default](https://github.com/alexarchambault/case-app/blob/master/core/shared/src/main/scala/caseapp/core/Default.scala)
type class

```tut:silent
case class Options(
  user: Option[String],
  enableFoo: Boolean = false,
  file: List[String] = Nil
)

CaseApp.parse[Options](Seq()) == Right((Options(None, false, Nil), Seq.empty))
```

```tut:invisible
assert(
  CaseApp.parse[Options](Seq()) == Right((Options(None, false, Nil), Seq.empty))
)
```

### Lists

Some arguments can be specified several times on the command-line. These
should be typed as lists, e.g. `file` in

```tut:silent
case class Options(
  user: Option[String],
  enableFoo: Boolean = false,
  file: List[String]
)

CaseApp.parse[Options](
  Seq("--file", "a", "--file", "b")
) == Right((Options(None, false, List("a", "b")), Seq.empty))
```

```tut:invisible
assert(
  CaseApp.parse[Options](
    Seq("--file", "a", "--file", "b")
  ) == Right((Options(None, false, List("a", "b")), Seq.empty))
)
```

If an argument is specified several times, but is not typed as a `List` (or an accumulating type,
see below), the final value of its corresponding field is the last provided in the arguments.

### Whole application with argument parsing

*case-app* can take care of the creation of the `main` method parsing
command-line arguments.

```tut:silent
import caseapp._

case class ExampleOptions(
  foo: String,
  bar: Int
)

object Example extends CaseApp[ExampleOptions] {

  def run(options: ExampleOptions, arg: RemainingArgs): Unit = {
    // Core of the app
    // ...
  }

}
```

`Example` in the above example will then have a `main` method, parsing
the arguments it is given to an `ExampleOptions`, then calling the `run` method
if parsing was successful.

### Automatic help and usage options

Running the above example with the `--help` (or `-h`) option will print an help message
of the form
```
Example
Usage: example [options]
  --foo  <value>
  --bar  <value>
```

```tut:invisible
def lines(s: String) = s.linesIterator.toVector
assert(
  lines(CaseApp.helpMessage[ExampleOptions]).drop(2) == lines(
  """Example
    |Usage: example [options]
    |  --foo  <value>
    |  --bar  <value>""".stripMargin
  ).drop(2)
)
```

Calling it with the `--usage` option will print
```
Usage: example [options]
```

### Customizing items of the help / usage message

Several parts of the above help message can be customized by annotating
`ExampleOptions` or its fields:

```tut:silent
@AppName("MyApp")
@AppVersion("0.1.0")
@ProgName("my-app-cli")
case class ExampleOptions(
  @HelpMessage("the foo")
  @ValueDescription("foo")
    foo: String,
  @HelpMessage("the bar")
  @ValueDescription("bar")
    bar: Int
)
```

```tut:invisible
object Example extends CaseApp[ExampleOptions] {
  def run(options: ExampleOptions, args: RemainingArgs): Unit = {
    // Core of the app
    // ...
  }
}
```

Called with the `--help` or `-h` option, would print
```
MyApp 0.1.0
Usage: my-app-cli [options]
  --foo  <foo>: the foo
  --bar  <bar>: the bar
```

```tut:invisible
assert(
  lines(CaseApp.helpMessage[ExampleOptions]) == lines(
  """MyApp 0.1.0
    |Usage: my-app-cli [options]
    |  --foo  <foo>
    |        the foo
    |  --bar  <bar>
    |        the bar""".stripMargin
  )
)
```

Note the application name that changed, on the first line. Note also the version
number appended next to it. The program name, after `Usage: `, was changed too.

Lastly, the options value descriptions (`<foo>` and `<bar>`) and help messages
(`the foo` and `the bar`), were customized.

### Extra option names

Alternative option names can be specified, like
```tut:silent
case class ExampleOptions(
  @ExtraName("f")
    foo: String,
  @ExtraName("b")
    bar: Int
)
```

```tut:invisible
object Example extends CaseApp[ExampleOptions] {
  def run(options: ExampleOptions, args: RemainingArgs): Unit = {}
}
```

`--foo` and `-f`, and `--bar` and `-b` would then be equivalent.

### Long / short options

Field names, or extra names as above, longer than one letter are considered
long options, prefixed with `--`. One letter long names are short options,
prefixed with a single `-`.

```tut:silent
case class ExampleOptions(
  a: Int,
  foo: String
)
```

```tut:invisible
object Example extends CaseApp[ExampleOptions] {
  def run(options: ExampleOptions, args: RemainingArgs): Unit = {}
}
```

would accept `--foo bar` and `-a 2` as arguments to set `foo` or `a`.

### Pascal case conversion

Field names or extra names as above, written in pascal case, are split
and hyphenized.

```tut:silent
case class Options(
  fooBar: Double
)
```

```tut:invisible
core.Parser[Options]
```

would accept arguments like `--foo-bar 2.2`.


### Reusing options

Sets of options can be shared between applications:

```tut:silent
case class CommonOptions(
  foo: String,
  bar: Int
)

case class First(
  baz: Double,
  @Recurse
    common: CommonOptions
) {

  // ...

}

case class Second(
  bas: Long,
  @Recurse
    common: CommonOptions
) {

  // ...

}
```

```tut:invisible
core.Parser[First]
core.Parser[Second]
```

### Commands

*case-app* has a support for commands.

```tut:silent
sealed trait DemoCommand

case class First(
  foo: Int,
  bar: String
) extends DemoCommand

case class Second(
  baz: Double
) extends DemoCommand

object MyApp extends CommandApp[DemoCommand] {
  def run(command: DemoCommand, args: RemainingArgs): Unit = {}
}
```

`MyApp` can then be called with arguments like
```
my-app first --foo 2 --bar a
my-app second --baz 2.4
```

- help messages
- customization
- base command
- ...

### Counters

*Needs to be updated*

Some more complex options can be specified multiple times on the command-line and should be
"accumulated". For example, one would want to define a verbose option like
```tut:silent
case class Options(
  @ExtraName("v") verbose: Int
)
```

```tut:invisible
Parser[Options]
```

Verbosity would then have be specified on the command-line like `--verbose 3`.
But the usual preferred way of increasing verbosity is to repeat the verbosity
option, like in `-v -v -v`. To accept the latter,
tag `verbose` type with `Counter`:
```tut:silent
case class Options(
  @ExtraName("v") verbose: Int @@ Counter
)
```

`verbose` (and `v`) option will then be viewed as a flag, and the
`verbose` variable will contain
the number of times this flag is specified on the command-line.

It can optionally be given a default value other than 0. This
value will be increased by the number of times `-v` or `--verbose`
was specified in the arguments.


### User-defined option types

*Needs to be updated*

Use your own option types by defining implicit `ArgParser`s for them, like in
```tut:silent
import caseapp.core.ArgParser

trait Custom

implicit val customArgParser: ArgParser[Custom] =
  ArgParser.instance[Custom]("custom") { s =>
    // parse s
    // return
    // - Left("error message") in case of error
    // - Right(custom) in case of success
    ???
  }
```

Then use them like
```tut:silent
case class Options(
  custom: Custom,
  foo: String
)
```

```tut:invisible
Parser[Options]
```

### Migration from the previous version

Shared options used to be automatic, and now require the `@Recurse`
annotation on the field corresponding to the shared options. This prevents
ambiguities with custom types as above.

## Usage

Add to your `build.sbt`
```scala
resolvers += Resolver.sonatypeRepo("releases")
libraryDependencies += "com.github.alexarchambault" %% "case-app" % "1.2.0-M2"
```

Note that case-app depends on shapeless 2.3. Use the `1.0.0` version if you depend on shapeless 2.2.

It is built against scala 2.10, 2.11, and 2.12.

If you are using scala 2.10.x, also add the macro paradise plugin to your build,
```scala
libraryDependencies +=
  compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
```

## Contributors

* Lars Hupel [@larsrh](https://github.com/larsrh)
* Martin Mauch [@nightscape](https://github.com/nightscape)
* Your name here :-)

## See also

Eugene Yokota, the current maintainer of scopt, and others, compiled
an (eeextremeeeely long) list of command-line argument parsing
libraries for Scala, in [this StackOverflow question](http://stackoverflow.com/questions/2315912/scala-best-way-to-parse-command-line-parameters-cli).

Unlike [scopt](https://github.com/scopt/scopt), case-app is less monadic / abstract data types based, and more
straight-to-the-point and descriptive / algebric data types oriented.
 
## Notice
 
Copyright (c) 2014-2017 Alexandre Archambault and contributors.
See LICENSE file for more details.

Released under Apache 2.0 license.


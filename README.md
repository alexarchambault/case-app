# case-app

*Type-level & seamless command-line argument parsing for Scala*

Just put your options in one or several case classes, like in
```scala
case class Options(
  user: Option[String],
  enableFoo: Boolean,
  files: List[String]
)
```
or
```scala
case class AuthOptions(
  user: String,
  password: String
)

case class PathOptions(
  @ExtraName("f") fooPath: String,
  @ExtraName("b") barPath: String
)

case class Options(
  auth: AuthOptions,
  paths: PathOptions
)
```

Parse them with
```scala
CaseApp.parse[Options](args) match {
  case Left(err) =>
    // Option parsing failed, error description in err
  case Right((options, remainingArgs)) =>
    // Success, we have options and remainingArgs
}
```

Alternately, accept help and usage options with
```scala
CaseApp.parseWithHelp[Options](args) match {
  case Left(err) =>
    // Option parsing failed, error description in err
  case Right((options, help, usage, remainingArgs)) =>
    // Success, we have options and remainingArgs

    // help and usage booleans tell us whether help or usage were requested
    if (help) {
      CaseApp.printHelp[Options]()
      sys exit 0
    }

    if (usage) {
      CaseApp.printUsage[Options]()
      sys exit 0
    }
}
```

Last refinement, you can also avoid the pain of having to define an
intermediary `scala.App` singleton or a `main` method with
```scala
case class MyApp(
  user: Option[String], // Nested case classes of options like above also accepted
  enableFoo: Boolean,
  @ExtraName("f") files: List[String]
) extends App { // Extending caseapp.App

  // core of your app, i.e. what would have been in main
  // remaining arguments are in
  //   def remainingArgs: Seq[String]

}

object MyApp extends AppOf[MyApp] {
  val parser = default
}
```

The singleton `MyApp` will then contain a main method that will:
- parse the command-line arguments,
- if requested, print a help or usage message and exit,
- run the content of a `MyApp` case class instance with the options
set, and the remaining arguments available through `remainingArgs`. This
uses `DelayedInit` under the hood.

`MyApp` can then by run with options, e.g.
```
--user aaa --enable-foo --file some_file extra_arg other_extra_arg
```
or
```
--user bbb -f first_file -f second_file
```

## Usage

Add to your `build.sbt`
```scala
libraryDependencies +=
  "com.github.alexarchambault" %% "case-app" % "0.2.0"
```

Import `caseapp._` (or just `caseapp.CaseApp` if you don't
need `caseapp.App` and annotations).

See above for usage examples.

## Features

### Default values

Options can be given default values. These will be kept
if no corresponding option was specified in the arguments.

Also, for options whose type does not match (hard-coded) usual types
like `Int`, `String`, etc.,
you must specify a default value for them. (This restriction
may be removed in the future - then parsing would simply fail
with a `Left` if no corresponding option was found in the arguments.
For now, a default value *must* be provided.)

Nested case classes of options should also be given a default value. (This
too should not be needed in the future).

### Extra names

Options can be given several names, by annotating the corresponding variable:
```scala
case class Options(
  @ExtraName("bar") foo: Option[String]
)
```

One letter names are assumed to be short options (name `"a"` will match option `-a`), longer names
are assumed to be long options (`"bar"` will match `"--bar"`).

Names are hyphenized and converted to lower case in order to get the corresponding option, e.g.
`"fooBar"` matches option `--foo-bar`.

### Nested option definitions

As illustrated above, case classes of options can be nested in other case classes
to facilitate re-usability of option definitions, like in:
```scala
case class CommonOptions(
  user: String,
  password: String
)

case class FirstAppOptions(
  common: CommonOptions,
  foo: Boolean
)

case class SecondAppOptions(
  common: CommonOptions,
  bar: Boolean
)
```

Options defined in `CommonOptions` are common to `FirstAppOptions`
and `SecondAppOptions`.

### Counters

Some more complex options can be specified multiple times on the command-line and should be
"accumulated". For example, one would want to define a verbose option like
```scala
case class Options(
  @ExtraName("v") verbose: Int
)
```

Verbosity would then have be specified on the command-line like `--verbose 3`.
But the usual preferred way of increasing verbosity is to repeat the verbosity
option, like in `-v -v -v`. To accept the latter,
tag `verbose` type with `Counter`:
```scala
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


### Lists

You may also expect a list of values for an option, like in
```scala
case class Options(
  list: List[String]
)
```

In this case, case-app will accumulate the options in a list:
`--list first --list second` will make
the `list` variable have the value `List("first", "second")`.

Here too, `list` can be given a non-`Nil` default value. Items from
the arguments will simply be appended to it.


### Help message and options

The help message generated by caseapp can be enhanced with
annotations, like in
```scala
  @AppName("Glorious App")
  @AppVersion("0.1.0")
case class AppOptions(
    @ValueDescription("a foo")
    @HelpMessage("Specify some foo")
  foo: Option[String],
    @ValueDescription("bar count")
    @HelpMessage("Specify the bar count")
  bar: Int
)
```
whose help message will be:
```
Glorious App 0.1.0
Usage: my-app [options]
  --foo  <a foo>
        Specify some foo
  --bar  <bar count>
        Specify the bar count
```
instead of the default
```
MyApp
Usage: my-app [options]
  --foo  <value>
  --bar  <value>
```

## Default supported option types

The following types are supported by default: `Boolean`, `Int`, `Long`, `Float`, `Double`,
`String`, `Calendar` (from `java.util`), `Unit` (ignored flag argument), and `Int @@ Counter` (see above), and
lists/options of these types.

## User-defined options types

Use your own option types by defining implicit `ArgParser`s for them, like in
```scala
implicit val customArgParser: ArgParser[Custom] = ArgParser.value[Custom] { (current, arg) =>
  // Current is the current value of this option
  // Return either:
  //   Success(newValue) if arg contains a valid value for a `Custom`
  //   Failure(reason)   else
}
```

## Internals

case-app uses the type class facilities from
[shapeless](https://github.com/milessabin/shapeless)
(mainly through `Lazy`). It also uses
a bit of reflection, mainly to get annotations and
default values of case classes parameters - macros (and more type classes and type level programming)
should be used instead in the future.

## TODO

Commands à la git or hadoop or like in scopt (called like *app* *command* *command arguments...*), defined as a (shapeless) union type
  whose keys are command names and values are case classes (as above) of the commands.
 
## See also

Eugene Yokota, the author of scopt, and others, compiled
an (eeextremeeeely long) list of command-line argument parsing
libraries for Scala, in [this StackOverflow question](http://stackoverflow.com/questions/2315912/scala-best-way-to-parse-command-line-parameters-cli).

Unlike [scopt](https://github.com/scopt/scopt), case-app is less monadic / abstract data types based, and more
straight-to-the-point and descriptive / algebric data types oriented.
 
## Notice
 
Copyright (c) 2014-2015 Alexandre Archambault.
See LICENSE file for more details.

Released under Apache 2.0 license.

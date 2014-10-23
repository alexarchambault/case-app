# case-app

*Seamless command-line argument parsing for Scala*

**case-app** parses command-line arguments. Possible options are simply variables of a case class, their types are validated
at compile-time via type-level programming, and these case classes can be defined recursively for more convenience (see below for more details).

## Getting started

Let's assume you have an application of the form
```scala
object MyProg extends App {

  // core of your app...

}
```

and you want to it to accept command-line arguments. Using *case-app*, you can just make the code above look like:
```scala    
import caseapp._

case class MyProg() extends App {

  // core of your app

}

object MyProg extends AppOf[MyProg] {
  val ignore = me
}
```
assuming you added the following lines to your `build.sbt`:
```
libraryDependencies += "com.github.alexarchambault" %% "case-app" % "0.1.0"
```

You can then add options to your program, for example:
```scala

case class MyProg(
  foo: Boolean = false,
  user: String,
  password: String,
  bar: Option[String]
) extends App {

  // core of your app, using the variables above

}
```

So transitioning from `scala.App` to case-app consists in importing `caseapp._`, making the former singleton
extending `App` (from the `scala` namespace) a case class extending `App` (from `caseapp`), and declaring a singleton with the same
name as the case class, extending `AppOf[`*it-self*`]` (and adding the line `val ignore = me` in it, see below for more
explanations).

Running this app with the `--help` option (like `sbt "run --help"`) will show a description of the available options.
Running it with unrecognized or malformed options will stop the program prematurely with an error message.

## Features

### Default values

Default values should be specified explicitly when defining the case class nesting the options. If they are not,
the main types supported by default are given hard-coded default values (`Int`: `0`, `Option[...]`: `None`, etc.).

### Extra names

Options can be given several names, by annotating the corresponding variable:
```scala
case class MyApp(
  @ExtraName("bar") foo: Option[String]
) extends App
```

One letter names are assumed to be short options (name `"a"` will match option `-a`), longer names
are assumed to be long options (`"bar"` will match `"--bar"`).

Name are converted to lower case and hyphenized in order to get the corresponding option, i.e.
`"fooBar"` matches option `--foo-bar`.

### Recursion

Option case classes can nest each other to facilitate re-usability of options definition, like in:
```scala
case class CommonOptions(
  user: String,
  password: String
)

case class FirstApp(
  common: CommonOptions
, foo: Boolean
) extends App {
  // First app content
}

case class SecondApp(
  common: CommonOptions
, bar: Boolean
) extends App {
  // Second app content...
}
```

Options defined in `CommonOptions` are common to `FirstApp` and `SecondApp`.

### Counters

Some more complex options can be specified multiple times on the command-line and should be
"accumulated". For example, one would want to define a verbose option like
```scala
case class MyApp(
  @ExtraName("v") verbose: Int
) extends App {
  // ...
}
```

Verbosity would then be specified on the command-line like `--verbose 3`. But this does not feel natural -
 the usual way of increasing verbosity is to repeat the verbosity option, which should be a flag, like in
 `-v -v -v`. To accept the latter (and only the latter), just tag `verbose` type with `Counter`:
```scala
case class MyApp(
  @ExtraName("v") verbose: Int @@ Counter
) extends App {
  // ...
}
```

`verbose` (and `v`) option will then be viewed as a flag, and the `verbose` variable will contain
the number of times this flag is specified on the command-line.


### Lists

Another kind of repeated options is when you expect a list of values, like in
```scala
case class MyApp(
  list: List[String]
) extends App {
  // ...
}
```

In this case, case-app will accumulate the options in a list: `--list first --list second` will make
the `list` variable have the value `List("first", "second")`.



### Help message and options

Running an app with the `--help` or `-h` options will make it print a help message, e.g. running
```scala
case class MyApp(
  foo: Option[String]
, bar: Int
) extends App {
  // ...
}
```

with `--help` option prints:
```
MyApp
Usage: my-app [options]
  --foo  <value>
  --bar  <value>

```

App name and version in the help message can be customized, as can the `<value>` argument value descriptors, and
per option help messages can be specified, e.g.
```scala
@AppName("Glorious App")
@AppVersion("0.1.0")
case class MyApp(
  @ValueDescription("a foo") @HelpMessage("Specify some foo") foo: Option[String]
, bar: Int
) extends App {
// ...
}
```
with `--help` option prints:
```
Glorious App 0.1.0
Usage: my-app [options]
  --foo  <a foo>
        Specify some foo
  --bar  <value>
```

### Getting an arguments parser

Argument parsers are also available as is:
```scala
    case class CC(foo: String, bar: Option[Int])

    val parser = implictly[Parser[CC]]
    parser(args) match {
      case Success((cc, remainingArgs)) =>
       // Success: we have a CC
      case Failure(t) =>
       // Failed: error is in t
    }
```


## Internals

* case-app uses type-level programming - with a slice of reflection - to achieve its goals.

It validates the option types at compile-time, using the well-known [shapeless](https://github.com/milessabin/shapeless)
along the way. It roughly processes the
case-class it is given, looking for an implicit `PreFolder[T]` for each of its variables - a `PreFolder[T]` basically
parses an option of type `T` out of arguments it is given, once it is given the name(s) of the option. It uses
shapeless'`LabelledProductTypeClass` mechanism.

* Why the `val ignore = me` ?

This line has to be added in the definitions of an `AppOf[CC]` in order to get some implicits about `CC`. These
cannot get along the default constructor of `AppOf` (defining `AppOf` like `AppOf[C <: ArgsApp](implicit ... some implicits...)`),
as one then runs into the same problem as [here](https://issues.scala-lang.org/browse/SI-5000)
and [here](https://issues.scala-lang.org/browse/SI-7666).

* Why depend on both shapeless *and* scalaz?

shapeless is used mainly for its `LabelledProductTypeClass` mechanism. scalaz is used for its `Tag` mechanism -
shapeless has a `Tag` mechanism as well, but one runs into the same problem as [here](https://github.com/scalaz/scalaz/issues/747)
with it - this problem is fixed in scalaz 7.1, so we use the latter.
     
* case-app uses `DelayedInit`, like `scala.App`

It does this in order to delay the execution of the content
of the classes extending `caseapp.App`. `DelayedInit` is deprecated, but support should
continue until a viable alternative exists, as mentioned [here](https://github.com/scala/scala/releases/tag/v2.11.0-RC1).
 
## TODO

* Default values with system config (through typesafe-config): if an option was not specified on the command-line and
  a value for it is available through the config, use the former instead of the default value in the case class definition
  or one deduced from the option type.
* Commands à la git or hadoop or like in scopt (called like *app* *command* *command arguments...*), defined as a (shapeless) record type
  whose keys are command names and values are case classes (as above) of the commands.
 
## See also

Eugene Yokota, the author of scopt, and others, compiled a list of command-line argument parsing
library for Scala, in [this StackOverflow question](http://stackoverflow.com/questions/2315912/scala-best-way-to-parse-command-line-parameters-cli).

Unlike [scopt](https://github.com/scopt/scopt), case-app is less monadic / abstract data types based, and more
descriptive / algebric data types oriented.
 
## Notice
 
Copyright (c) 2014 Alexandre Archambault. See LICENSE file for more details.

Released under Apache 2.0 license.

# Commands

The use of commands relies on the same API as [`CaseApp`](parse.md#application-definition).
While it is possible to use the command argument parser
[in a standalone fashion](#standalone-use-of-the-command-argument-parser),
sections below assume you're parsing options by
extending `CaseApp` (or its `Command` sub-class).

## Defining commands

```scala mdoc:reset-object:invisible
import caseapp._
```

Individual commands are defined as instances of `Command`, which is
itself a sub-class of `CaseApp`. `Command` adds a few methods to `CaseApp`,
that can be overridden, most notably `name` and `names`.

```scala mdoc:silent
case class FirstOptions(
  foo: String = ""
)

object First extends Command[FirstOptions] {
  override def names = List(
    List("first"),
    List("frst"),
    List("command-one"),
    List("command", "one")
  )
  def run(options: FirstOptions, args: RemainingArgs): Unit = {
    ???
  }
}

case class SecondOptions(
  foo: String = ""
)

object Second extends Command[SecondOptions] {
  override def name = "command-two"
  def run(options: SecondOptions, args: RemainingArgs): Unit = {
    ???
  }
}
```

Individual commands are gathered in an object extending `CommandsEntryPoint`:
```scala mdoc:silent
object MyApp extends CommandsEntryPoint {
  def progName = "my-app"
  def commands = Seq(
    First,
    Second
  )
}
```

## Customizing commands help

## Enabling support for completion

See [completion support](completion.md)

## Advanced

### Hidden commands

```scala mdoc:reset-object:invisible
import caseapp._
```

Overriding the `def hidden: Boolean` method of `Command` allows to hide a
command from the help message:
```scala mdoc:silent
case class FirstOptions()

object First extends Command[FirstOptions] {
  def run(options: FirstOptions, args: RemainingArgs) = {
    ???
  }
}

case class SecondOptions()

object Second extends Command[SecondOptions] {

  // hide this command from the command listing in the help message
  override def hidden = true

  def run(options: SecondOptions, args: RemainingArgs) = {
    ???
  }
}

object MyApp extends CommandsEntryPoint {
  def progName = "my-app"
  def commands = Seq(
    First,
    Second
  )
}
```

One gets as a help message:
```scala mdoc:passthrough
println("```text")
println {
  // FIXME Find a way to keep colors in the output
  MyApp.help.help(
    // reset colors
    MyApp.helpFormat
      .withProgName(caseapp.core.util.fansi.Attrs.Empty)
      .withCommandName(caseapp.core.util.fansi.Attrs.Empty)
      .withOption(caseapp.core.util.fansi.Attrs.Empty)
      .withHidden(caseapp.core.util.fansi.Attrs.Empty)
  )
}
println("```")
```

### Command groups

```scala mdoc:reset-object:invisible
import caseapp._
```

Override the `def group: String` method of `Command` to gather
similar commands together in the help message listing commands:
```scala mdoc:silent

case class FirstOptions()

object First extends Command[FirstOptions] {
  override def group = "Main"
  def run(options: FirstOptions, args: RemainingArgs) = {
    ???
  }
}

case class SecondOptions()

object Second extends Command[SecondOptions] {
  override def group = "Other"
  def run(options: SecondOptions, args: RemainingArgs) = {
    ???
  }
}

object MyApp extends CommandsEntryPoint {
  override def defaultCommand = None
  def progName = "my-app"
  def commands = Seq(
    First,
    Second
  )
}
```

One gets as a help message:
```scala mdoc:passthrough
println("```text")
println {
  // FIXME Find a way to keep colors in the output
  MyApp.help.help(
    // reset colors
    MyApp.helpFormat
      .withProgName(caseapp.core.util.fansi.Attrs.Empty)
      .withCommandName(caseapp.core.util.fansi.Attrs.Empty)
      .withOption(caseapp.core.util.fansi.Attrs.Empty)
      .withHidden(caseapp.core.util.fansi.Attrs.Empty)
  )
}
println("```")
```

To sort groups, set the `sortCommandGroups` or `sortedCommandGroups` command
fields of `Command#helpFormat`, like
```scala mdoc:silent
object MyOtherApp extends CommandsEntryPoint {
  override def defaultCommand = None
  def progName = "my-other-app"
  override def helpFormat = super.helpFormat.withSortedCommandGroups(
    Some(Seq("Other", "Main"))
  )
  def commands = Seq(
    First,
    Second
  )
}
```

One then gets as a help message:
```scala mdoc:passthrough
println("```text")
println {
  // FIXME Find a way to keep colors in the output
  MyOtherApp.help.help(
    // reset colors
    MyOtherApp.helpFormat
      .withProgName(caseapp.core.util.fansi.Attrs.Empty)
      .withCommandName(caseapp.core.util.fansi.Attrs.Empty)
      .withOption(caseapp.core.util.fansi.Attrs.Empty)
      .withHidden(caseapp.core.util.fansi.Attrs.Empty)
  )
}
println("```")
```

### Standalone use of the command argument parser

```scala mdoc:reset:invisible
import caseapp._
```

Use one of the overrides of `RuntimeCommandParser.parse` to parse a list of arguments
to a command, like
```scala mdoc:silent
import caseapp.core.commandparser.RuntimeCommandParser

case class MyCommand(name: String)

val commandMap = Map(
  List("first") -> MyCommand("First one"),
  List("second") -> MyCommand("Second one"),
  List("the", "first") -> MyCommand("First one"),
  List("the", "second") -> MyCommand("Second one")
)
```

```scala mdoc
// no default command
RuntimeCommandParser.parse[MyCommand](
  commandMap,
  List("the", "first", "a", "--thing", "--foo", "b")
)

// override accepting a default command
RuntimeCommandParser.parse[MyCommand](
  MyCommand("Default one"),
  commandMap,
  List("the", "thing", "a", "--thing", "--foo", "b")
)

RuntimeCommandParser.parse[MyCommand](
  MyCommand("Default one"),
  commandMap,
  List("first", "a", "--thing", "--foo", "b")
)
```

Note that there are also overrides accepting a `Seq[Command[_]]` rather
than a map like `commandMap` above, that build the command map out of the `Command[_]`
sequence.

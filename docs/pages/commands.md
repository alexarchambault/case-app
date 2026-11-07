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

### Default command names and descriptions

```scala mdoc:reset-object:invisible
import caseapp._
```

If neither `name` nor `names` are overridden, a command is named after the
program name of its options, that is the value of the `@ProgName` annotation of
the options class if it has one, or the name of the options class, minus its
`Options` suffix, converted to kebab case. Put a `@HelpMessage` annotation on
the options class to give a description to the command, whose first line
is shown in the list of commands of the help message (see
[application description](help.md#application-description)):
```scala mdoc:silent
@HelpMessage("Add a remote")
case class RemoteAddOptions(
  url: String = ""
)

object RemoteAdd extends Command[RemoteAddOptions] {
  def run(options: RemoteAddOptions, args: RemainingArgs): Unit = {
    ???
  }
}

@ProgName("rm")
@HelpMessage("Remove a remote")
case class RemoveOptions(
  force: Boolean = false
)

object Remove extends Command[RemoveOptions] {
  def run(options: RemoveOptions, args: RemainingArgs): Unit = {
    ???
  }
}

object MyApp extends CommandsEntryPoint {
  def progName = "my-app"
  def commands = Seq(
    RemoteAdd,
    Remove
  )
}
```

```scala mdoc
RemoteAdd.names
Remove.names
```

```scala mdoc:passthrough
// FIXME Find a way to keep colors in the output
import caseapp.core.help.HelpFormat
println("```text")
println(MyApp.help.help(HelpFormat.default(ansiColors = false)))
println("```")
```

Note that the `@CommandName` annotation, that can be put on options classes,
isn't used by case-app itself to name commands: override `name` or `names` instead.

## Customizing commands help

```scala mdoc:reset-object:invisible
import caseapp._
```

The help message of a `CommandsEntryPoint`, printed when no command is passed
(and there's no default command), lists the available commands. Each command is
described by the first line of the `@HelpMessage` annotation of its options class.
On the `CommandsEntryPoint` side, override `description` to print a text
right after the usage line, and `summaryDesc` to print a text at the end of the
help message:
```scala mdoc:silent
@HelpMessage("Build things")
case class BuildOptions(
  foo: String = ""
)

object Build extends Command[BuildOptions] {
  def run(options: BuildOptions, args: RemainingArgs): Unit = {
    ???
  }
}

@HelpMessage("Clean things\nDetails that only appear in the help message of clean")
case class CleanOptions(
  bar: String = ""
)

object Clean extends Command[CleanOptions] {
  def run(options: CleanOptions, args: RemainingArgs): Unit = {
    ???
  }
}

object MyApp extends CommandsEntryPoint {
  def progName = "my-app"
  override def description = "My app does things."
  override def summaryDesc = "Run my-app <COMMAND> --help for more details."
  def commands = Seq(
    Build,
    Clean
  )
}
```

One gets as a help message:
```scala mdoc:passthrough
import caseapp.core.help.HelpFormat
println("```text")
println(MyApp.help.help(HelpFormat.default(ansiColors = false)))
println("```")
```

The format of that help message (colors, [command group](#command-groups) order, …)
can be customized by overriding `def helpFormat: HelpFormat` in the `CommandsEntryPoint`.
The help message of each command is that of its options, and can be customized like
[that of a `CaseApp`](help.md).

## Enabling support for completion

See [completion support](completion.md)

## Advanced

### Hidden commands

```scala mdoc:reset-object:invisible
import caseapp._
```

Overriding the `def hidden: Boolean` method of `Command` hides a
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
      .copy(
        progName = caseapp.core.util.fansi.Attrs.Empty,
        commandName = caseapp.core.util.fansi.Attrs.Empty,
        option = caseapp.core.util.fansi.Attrs.Empty,
        hidden = caseapp.core.util.fansi.Attrs.Empty
      )
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
      .copy(
        progName = caseapp.core.util.fansi.Attrs.Empty,
        commandName = caseapp.core.util.fansi.Attrs.Empty,
        option = caseapp.core.util.fansi.Attrs.Empty,
        hidden = caseapp.core.util.fansi.Attrs.Empty
      )
  )
}
println("```")
```

To sort groups, override `helpFormat` in your `CommandsEntryPoint`, and set the
`sortedCommandGroups` field of `HelpFormat` (explicit group order) or its
`sortCommandGroups` field (function sorting group names), like
```scala mdoc:silent
object MyOtherApp extends CommandsEntryPoint {
  override def defaultCommand = None
  def progName = "my-other-app"
  override def helpFormat = super.helpFormat.copy(
    sortedCommandGroups = Some(Seq("Other", "Main"))
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
      .copy(
        progName = caseapp.core.util.fansi.Attrs.Empty,
        commandName = caseapp.core.util.fansi.Attrs.Empty,
        option = caseapp.core.util.fansi.Attrs.Empty,
        hidden = caseapp.core.util.fansi.Attrs.Empty
      )
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

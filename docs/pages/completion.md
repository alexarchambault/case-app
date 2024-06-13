# Completion

## Enable support for completion

Support for completion relies on [commands](commands.md).

### In commands

In an application made of [commands](commands.md), enable completions by overriding
the `def enableCompletionsCommand: Boolean` and `def enableCompleteCommand: Boolean`.

This adds two (hidden) commands to your application:
- `completions` (also aliased to `completion`): command that allows to help installing
  completions
- `complete`: command run when users ask for completions in their shell

Overriding `def completionsWorkingDirectory: Option[String]` and returning a non-empty
value from it enables two more commands:
- `completions install` (also aliased to `completion install`): command to install completions
  for the current shell
- `completions uninstall` (also aliased to `completion uninstall`): command to uninstall
  completions for the current shell

### For simple applications

```scala mdoc:reset-object:invisible
import caseapp._
```

If you'd like to enable it in a simple application, make it extend
`Command` rather than `CaseApp`, and define a `CommandsEntryPoint`
with no commands, and your application as default command:
```scala mdoc:silent
case class Options(
  foo: String = ""
)

object MyActualApp extends Command[Options] {
  def run(options: Options, args: RemainingArgs): Unit = {
    ???
  }
}

object MyApp extends CommandsEntryPoint {
  def progName = "my-app"
  def commands = Seq()
  override def defaultCommand = Some(MyActualApp)
  override def enableCompleteCommand = true
  override def enableCompletionsCommand = true
}
```

## Install completions

### Via `completions install`

Assuming `progname` runs the main class added by `CommandEntryPoint` to the object extended
by it, you can install completions with
```text
$ progname completions install
```

```scala mdoc:passthrough
println("```text")
for (line <- MyApp.completionsInstalledMessage("~/.zshrc", updated = false))
  println(line)
println("```")
```

## Get completions

The file installed by `completions install` above runs your application
to get completions. It runs it via the `complete` command, like
```text
$ my-app complete zsh-v1 2 my-app -
```

```scala mdoc:passthrough
println("```text")
MyApp.main(Array("complete", "zsh-v1", "2", "my-app", "-"))
println("```")
```

Usage:
```scala mdoc:passthrough
println("```text")
MyApp.main(Array("complete", "--help"))
println("```")
```

## Provide completions for individual option values

```scala mdoc:reset-object:invisible
import caseapp._
```

```scala mdoc:silent
import caseapp.core.complete.CompletionItem

case class Options(
  foo: String = ""
)

object MyActualApp extends Command[Options] {
  def run(options: Options, args: RemainingArgs): Unit = {
    ???
  }

  override def completer =
    super.completer.completeOptionValue {
      val hardCodedValues = List(
        "aaa",
        "aab",
        "aac",
        "abb"
      )
      (arg, prefix, state, args) =>
        // provide hard-coded values as completions for --foo values
        if (arg.names.map(_.name).contains("foo")) {
          val items = hardCodedValues.filter(_.startsWith(prefix)).map { value =>
            CompletionItem(value)
          }
          Some(items)
        }
        else
          None
    }
}

object MyApp extends CommandsEntryPoint {
  def progName = "my-app"
  def commands = Seq()
  override def defaultCommand = Some(MyActualApp)
  override def enableCompleteCommand = true
  override def enableCompletionsCommand = true
}
```

One can then get specific completions, like
```text
$ my-app complete zsh-v1 3 my-app --foo a
```

```scala mdoc:passthrough
println("```text")
MyApp.main(Array("complete", "zsh-v1", "3", "my-app", "--foo", "a"))
println("```")
```

```text
$ my-app complete zsh-v1 3 my-app --foo aa
```

```scala mdoc:passthrough
println("```text")
MyApp.main(Array("complete", "zsh-v1", "3", "my-app", "--foo", "aa"))
println("```")
```

package caseapp.core.complete

import caseapp.{HelpMessage, Name}
import caseapp.core.help.Help
import caseapp.core.parser.Parser

// from https://github.com/VirtusLab/scala-cli/blob/eced0b35c769eca58ae6f1b1a3be0f29a8700859/modules/cli/src/main/scala/scala/cli/commands/installcompletions/InstallCompletionsOptions.scala
// format: off
final case class CompletionsInstallOptions(
  @HelpMessage("Print completions to stdout")
    env: Boolean = false,
  @HelpMessage("Custom completions name")
    name: Option[String] = None,
  @HelpMessage("Name of the shell, either zsh, fish or bash")
  @Name("shell")
    format: Option[String] = None,
  @HelpMessage("Completions output directory (defaults to $XDG_CONFIG_HOME/fish/completions on fish)")
  @Name("o")
    output: Option[String] = None,
  @HelpMessage("Custom banner in comment placed in rc file (bash or zsh only)")
    banner: String = "{NAME} completions",
  @HelpMessage("Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell (bash or zsh only)")
    rcFile: Option[String] = None
)
// format: on

object CompletionsInstallOptions {
  implicit lazy val parser: Parser[CompletionsInstallOptions] = Parser.derive
  implicit lazy val help: Help[CompletionsInstallOptions]     = Help.derive
}

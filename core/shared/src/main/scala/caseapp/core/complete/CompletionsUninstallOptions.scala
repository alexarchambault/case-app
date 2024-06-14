package caseapp.core.complete

import caseapp.{HelpMessage, Name}
import caseapp.core.help.Help
import caseapp.core.parser.Parser

// from https://github.com/VirtusLab/scala-cli/blob/eced0b35c769eca58ae6f1b1a3be0f29a8700859/modules/cli/src/main/scala/scala/cli/commands/uninstallcompletions/SharedUninstallCompletionsOptions.scala
// format: off
final case class CompletionsUninstallOptions(
  @HelpMessage("Path to `*rc` file, defaults to `.bashrc` or `.zshrc` depending on shell (bash or zsh only)")
    rcFile: Option[String] = None,
  @HelpMessage("Custom banner in comment placed in rc file")
    banner: String = "{NAME} completions",
  @HelpMessage("Custom completions name")
    name: Option[String] = None,
  @HelpMessage("Completions output directory (defaults to $XDG_CONFIG_HOME/fish/completions on fish)")
  @Name("o")
    output: Option[String] = None,
)
// format: on

object CompletionsUninstallOptions {
  implicit lazy val parser: Parser[CompletionsUninstallOptions] = Parser.derive
  implicit lazy val help: Help[CompletionsUninstallOptions]     = Help.derive
}

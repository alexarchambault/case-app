package caseapp.core.help

import caseapp.core.Arg
import caseapp.core.app.CaseApp
import shapeless._

final class CommandsHelpOps[C <: Coproduct](val commandsHelp: CommandsHelp[C]) extends AnyVal {

  def add[H](
    name: String,
    args: Seq[Arg],
    argsNameOption: Option[String]
  ): CommandsHelp[H :+: C] =
    CommandsHelp(
      commandsHelp.messages :+ (Seq(name) -> CommandHelp(args, argsNameOption))
    )

  def add[H](
    command: CaseApp[H],
    name: String = ""
  ): CommandsHelp[H :+: C] =
    add(
      if (name.isEmpty) command.messages.progName else name,
      command.parser.args,
      command.messages.argsNameOption
    )

  def reverse[R <: Coproduct](implicit rev: ops.coproduct.Reverse.Aux[C, R]): CommandsHelp[R] =
    commandsHelp.as[R]

}

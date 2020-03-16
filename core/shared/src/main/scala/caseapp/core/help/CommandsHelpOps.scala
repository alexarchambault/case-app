package caseapp.core.help

import caseapp.core.Arg
import caseapp.core.app.CaseApp
import shapeless._
import caseapp.HelpMessage

final class CommandsHelpOps[C <: Coproduct](val commandsHelp: CommandsHelp[C]) extends AnyVal {

  def add[H](
    name: String,
    args: Seq[Arg],
    argsNameOption: Option[String],
    helpMessage: Option[HelpMessage]
  ): CommandsHelp[H :+: C] =
    CommandsHelp(
      commandsHelp.messages :+ (Seq(name) -> CommandHelp(args, argsNameOption, helpMessage))
    )

  def add[H](
    command: CaseApp[H],
    name: String = "",
    helpMessage: Option[HelpMessage] = None
  ): CommandsHelp[H :+: C] =
    add(
      if (name.isEmpty) command.messages.progName else name,
      command.parser.args,
      command.messages.argsNameOption,
      helpMessage
    )

  def reverse[R <: Coproduct](implicit rev: ops.coproduct.Reverse.Aux[C, R]): CommandsHelp[R] =
    commandsHelp.as[R]

}

package caseapp.core.help

import java.util.Locale

import caseapp.core.util.CaseUtil
import caseapp.{ArgsName, CommandName, Parser}
import caseapp.util.AnnotationOption
import dataclass.data
import shapeless.{:+:, CNil, Coproduct, LabelledGeneric, Strict, Witness}
import shapeless.labelled.FieldType

import scala.language.implicitConversions


@data class CommandsHelp[T](
  messages: Seq[(Seq[String], CommandHelp)]
) {
  lazy val messagesMap = messages.toMap

  def as[U]: CommandsHelp[U] =
    CommandsHelp(messages)
}

object CommandsHelp {

  def apply[T](implicit messages: CommandsHelp[T]): CommandsHelp[T] = messages

  implicit val nil: CommandsHelp[CNil] =
    CommandsHelp(Nil)

  implicit def cons[K <: Symbol, H, T <: Coproduct]
   (implicit
     key: Witness.Aux[K],
     commandName: AnnotationOption[CommandName, H],
     parser: Strict[Parser[H]],
     argsName: AnnotationOption[ArgsName, H],
     tail: CommandsHelp[T]
   ): CommandsHelp[FieldType[K, H] :+: T] = {
    // FIXME Duplicated in CommandParser.ccons
    val name = commandName().map(_.commandName).getOrElse {
      CaseUtil.pascalCaseSplit(key.value.name.toList.takeWhile(_ != '$'))
        .map(_.toLowerCase(Locale.ROOT))
        .mkString("-")
    }

    CommandsHelp((Seq(name) -> CommandHelp(
      parser.value.args,
      argsName().map(_.argsName)
    )) +: tail.messages)
  }

  implicit def generic[S, C <: Coproduct]
   (implicit
     gen: LabelledGeneric.Aux[S, C],
     underlying: Strict[CommandsHelp[C]]
   ): CommandsHelp[S] =
    CommandsHelp(underlying.value.messages)


  implicit def toCommandsHelpOps[T <: Coproduct](commandsHelp: CommandsHelp[T]): CommandsHelpOps[T] =
    new CommandsHelpOps(commandsHelp)

}

package caseapp.core.help

import caseapp.core.util.CaseUtil
import caseapp.{ArgsName, CommandName, Parser}
import caseapp.util.AnnotationOption
import shapeless.{:+:, CNil, Coproduct, LabelledGeneric, Strict, Witness}
import shapeless.labelled.FieldType


final case class CommandsHelp[T](
  messages: Seq[(String, CommandHelp)]
) {
  lazy val messagesMap = messages.toMap
}

object CommandsHelp {

  def apply[T](implicit messages: CommandsHelp[T]): CommandsHelp[T] = messages

  implicit val cnil: CommandsHelp[CNil] =
    CommandsHelp(Nil)

  implicit def ccons[K <: Symbol, H, T <: Coproduct]
   (implicit
    key: Witness.Aux[K],
    commandName: AnnotationOption[CommandName, H],
    parser: Strict[Parser[H]],
    argsName: AnnotationOption[ArgsName, T],
    tail: CommandsHelp[T]
   ): CommandsHelp[FieldType[K, H] :+: T] = {
    // FIXME Duplicated in CommandParser.ccons
    val name = commandName().map(_.commandName).getOrElse {
      CaseUtil.pascalCaseSplit(key.value.name.toList.takeWhile(_ != '$'))
        .map(_.toLowerCase)
        .mkString("-")
    }

    CommandsHelp((name -> CommandHelp(
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
}

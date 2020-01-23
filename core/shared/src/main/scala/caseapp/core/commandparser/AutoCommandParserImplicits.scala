package caseapp.core.commandparser

import caseapp.CommandName
import caseapp.core.parser.Parser
import caseapp.core.util.CaseUtil
import caseapp.util.AnnotationOption
import shapeless.{:+:, CNil, Coproduct, LabelledGeneric, Strict, Witness}
import shapeless.labelled.{FieldType, field}

abstract class AutoCommandParserImplicits {

  implicit def cnil: CommandParser[CNil] =
    NilCommandParser

  implicit def ccons[K <: Symbol, H, T <: Coproduct]
   (implicit
    key: Witness.Aux[K],
    commandName: AnnotationOption[CommandName, H],
    parser: Strict[Parser[H]],
    tail: CommandParser[T]
   ): CommandParser[FieldType[K, H] :+: T] = {

    val name = commandName().map(_.commandName).getOrElse {
      CaseUtil.pascalCaseSplit(key.value.name.toList.takeWhile(_ != '$'))
        .map(_.toLowerCase)
        .mkString("-")
    }

    ConsCommandParser(Seq(name), parser.value, tail)
      .mapHead(field[K](_))
  }

  implicit def generic[S, C <: Coproduct]
   (implicit
    lgen: LabelledGeneric.Aux[S, C],
    underlying: Strict[CommandParser[C]]
   ): CommandParser[S] =
    underlying.value.map(lgen.from)

}

package caseapp.core.commandparser

import caseapp.core.parser.Parser
import dataclass.data
import shapeless.{:+:, Coproduct, Inl, Inr}

@data class ConsCommandParser[H, T <: Coproduct](
  name: Seq[String],
  parser: Parser[H],
  tail: CommandParser[T]
) extends CommandParser[H :+: T] {

  private val tail0 = tail.map[H :+: T](Inr(_))

  def commandMap: Map[Seq[String], Parser[H :+: T]] =
    tail0.commandMap + (name -> parser.map[H :+: T](Inl(_)))

  def mapHead[I](f: H => I): CommandParser[I :+: T] =
    map {
      case Inl(h) => Inl(f(h))
      case Inr(t) => Inr(t)
    }

}

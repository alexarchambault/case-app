package caseapp.core.commandparser

import caseapp.core.parser.Parser
import shapeless.{:+:, Coproduct, Inl, Inr}

final case class ConsCommandParser[H, T <: Coproduct](
  name: String,
  parser: Parser[H],
  tail: CommandParser[T]
) extends CommandParser[H :+: T] {

  private val tail0 = tail.map[H :+: T](Inr(_))

  def get(command: String): Option[Parser[H :+: T]] =
    if (command == name)
      Some(parser.map(h => Inl(h)))
    else
      tail0.get(command)

  def mapHead[I](f: H => I): CommandParser[I :+: T] =
    map {
      case Inl(h) => Inl(f(h))
      case Inr(t) => Inr(t)
    }

}

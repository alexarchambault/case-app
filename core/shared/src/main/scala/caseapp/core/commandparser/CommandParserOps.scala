package caseapp.core.commandparser

import caseapp.core.app.CaseApp
import caseapp.core.parser.Parser
import shapeless.{:+:, Coproduct, Generic, ops}

@deprecated("Use Command and CommandsEntryPoint instead", "2.1.0")
final class CommandParserOps[T <: Coproduct](val commandParser: CommandParser[T]) extends AnyVal {

  // foo added not to collide with the other add method below
  def add[H](name: String)(implicit parser: Parser[H], foo: Unit = ()): CommandParser[H :+: T] =
    add(Seq(name))

  def add[H](name: Seq[String])(implicit
    parser: Parser[H],
    unused: Parser[H]
  ): CommandParser[H :+: T] =
    ConsCommandParser(name, parser, commandParser)

  def add[H](name: String, parser: Parser[H]): CommandParser[H :+: T] =
    add(Seq(name), parser)

  def add[H](name: Seq[String], parser: Parser[H]): CommandParser[H :+: T] =
    ConsCommandParser(name, parser, commandParser)

  def add[H](
    app: CaseApp[H]
  ): CommandParser[H :+: T] =
    add(app, app.messages.progName)

  def add[H](
    app: CaseApp[H],
    name: String
  ): CommandParser[H :+: T] =
    add(app, Seq(name))

  def add[H](
    app: CaseApp[H],
    name: Seq[String]
  ): CommandParser[H :+: T] =
    ConsCommandParser(
      if (name.isEmpty) Seq(app.messages.progName) else name,
      app.parser,
      commandParser
    )

  def as[F](implicit helper: CommandParserOps.AsHelper[T, F]): CommandParser[F] =
    helper(commandParser)

  def reverse[R <: Coproduct](implicit rev: ops.coproduct.Reverse.Aux[T, R]): CommandParser[R] =
    commandParser.map(rev.apply)

}

object CommandParserOps {

  sealed abstract class AsHelper[T, F] {
    def apply(parser: CommandParser[T]): CommandParser[F]
  }

  implicit def defaultAsHelper[F, T <: Coproduct, R <: Coproduct](implicit
    gen: Generic.Aux[F, R],
    rev: ops.coproduct.Reverse.Aux[T, R]
  ): AsHelper[T, F] =
    new AsHelper[T, F] {
      def apply(parser: CommandParser[T]) =
        parser
          .map(rev.apply)
          .map(gen.from)
    }

}

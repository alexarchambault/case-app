package caseapp.core

import caseapp.CommandName
import caseapp.core.util.pascalCaseSplit
import derive.AnnotationOption
import shapeless.labelled.{ FieldType, field }
import shapeless.{ :+:, Inl, Inr, Coproduct, CNil, Strict, LabelledGeneric, Witness }

trait CommandParser[T] {
  def get(command: String): Option[Parser[T]]

  def withHelp: CommandParser[WithHelp[T]] =
    CommandParser.instance { c =>
      get(c).map(_.withHelp)
    }

  def apply[D: Parser](args: Seq[String]): Either[String, (D, Seq[String], Option[Either[String, (String, T, Seq[String])]])] = {
    val dp = Parser[D]

    def helper(
      current: dp.D,
      args: Seq[String]
    ): Either[String, (D, Seq[String], Seq[String])] =
      if (args.isEmpty)
        dp.get(current).right.map((_, Nil, args))
      else
        dp.step(args, current) match {
          case Right(None) =>
            args match {
              case "--" :: t =>
                dp.get(current).right.map((_, t, Nil))
              case opt :: t if opt startsWith "-" =>
                Left(s"Unrecognized argument: $opt")
              case rem =>
                dp.get(current).right.map((_, Nil, rem))
            }

          case Right(Some((newD, newArgs))) =>
            assert(newArgs != args)
            helper(newD, newArgs)

          case Left(msg) =>
            Left(msg)
        }

    helper(dp.init, args.toList).right.map { case (d, dArgs, rem) =>
      val cmdOpt = rem.toList match {
        case c :: rem0 =>
          get(c) match {
            case None =>
              Some(Left(s"Command not found: $c"))
            case Some(p) =>
              Some(p(rem0).right.map { case (t, trem) => (c, t, trem) })
          }
        case Nil =>
          None
      }

      (d, dArgs, cmdOpt)
    }
  }

  def map[U](f: T => U): CommandParser[U] =
    CommandParser.instance { c =>
      get(c).map(_.map(f))
    }
}

object CommandParser {
  def apply[T](implicit parser: CommandParser[T]): CommandParser[T] = parser

  def instance[T](f: String => Option[Parser[T]]): CommandParser[T] =
    new CommandParser[T] {
      def get(command: String) = f(command)
    }

  implicit val cnil: CommandParser[CNil] =
    instance(_ => None)

  implicit def ccons[K <: Symbol, H, T <: Coproduct]
   (implicit
     key: Witness.Aux[K],
     commandName: AnnotationOption[CommandName, H],
     parser: Strict[Parser[H]],
     tail: CommandParser[T]
   ): CommandParser[FieldType[K, H] :+: T] =
    instance {
      val name = commandName().map(_.commandName).getOrElse {
        pascalCaseSplit(key.value.name.toList.takeWhile(_ != '$'))
          .map(_.toLowerCase)
          .mkString("-")
      }

      val tail0 = tail.map(Inr(_): FieldType[K, H] :+: T)

      c =>
        if (c == name)
          Some(parser.value.map(h => Inl(field[K](h))))
        else
          tail0.get(c)
    }

  implicit def generic[S, C <: Coproduct]
   (implicit
     lgen: LabelledGeneric.Aux[S, C],
     underlying: Strict[CommandParser[C]]
   ): CommandParser[S] =
    underlying.value.map(lgen.from)
}

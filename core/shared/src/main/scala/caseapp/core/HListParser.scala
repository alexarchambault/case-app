package caseapp
package core

import shapeless._
import shapeless.labelled.{ FieldType, field }

import caseapp.util.Implicit

trait HListParser[L <: HList, D <: HList, -N <: HList, -V <: HList, -M <: HList, -H <: HList, R <: HList] {
  type P <: HList
  def apply(default: D, names: N, valueDescriptions: V, helpMessages: M, noHelp: H): Parser.Aux[L, P]
}

object HListParser {
  def apply[L <: HList, D <: HList, N <: HList, V <: HList, M <: HList, H <: HList, R <: HList](implicit args: HListParser[L, D, N, V, M, H, R]): Aux[L, D, N, V, M, H, R, args.P] = args

  type Aux[L <: HList, D <: HList, N <: HList, V <: HList, M <: HList, H <: HList, R <: HList, P0 <: HList] =
    HListParser[L, D, N, V, M, H, R] { type P = P0 }

  def instance[L <: HList, D <: HList, N <: HList, V <: HList, M <: HList, H <: HList, R <: HList, P0 <: HList](p: (D, N, V, M, H) => Parser.Aux[L, P0]): Aux[L, D, N, V, M, H, R, P0] =
    new HListParser[L, D, N, V, M, H, R] {
      type P = P0
      def apply(default: D, names: N, valueDescriptions: V, helpMessages: M, noHelp: H) = p(default, names, valueDescriptions, helpMessages, noHelp)
    }

  implicit val hnil: Aux[HNil, HNil, HNil, HNil, HNil, HNil, HNil, HNil] =
    instance { (_, _, _, _, _) =>
      new Parser[HNil] {
        type D = HNil
        def init = HNil
        def step(args: Seq[String], d: HNil) = Right(None)
        def get(d: HNil) = Right(HNil)
        def args = Vector.empty
      }
    }

  implicit def hconsTaggedDefault[K <: Symbol, Tag, H, T <: HList, PT <: HList, DT <: HList, NT <: HList, VT <: HList, MT <: HList, HT <: HList, RT <: HList]
   (implicit
    name: Witness.Aux[K],
    argParser: Lazy[ArgParser[H @@ Tag]],
    headDefault: Implicit[Option[Default[H @@ Tag]]],
    tail: Lazy[Aux[T, DT, NT, VT, MT, HT, RT, PT]]
   ): Aux[FieldType[K, H @@ Tag] :: T, Option[H @@ Tag] :: DT, List[Name] :: NT, Option[ValueDescription] :: VT, Option[HelpMessage] :: MT, Option[Hidden] :: HT, None.type :: RT, Option[H @@ Tag] :: PT] =
    hconsDefault[K, H @@ Tag, T, PT, DT, NT, VT, MT, HT, RT]

  implicit def hconsDefault[K <: Symbol, H, T <: HList, PT <: HList, DT <: HList, NT <: HList, VT <: HList, MT <: HList, HT <: HList, RT <: HList]
   (implicit
    name: Witness.Aux[K],
    argParser: Lazy[ArgParser[H]],
    headDefault: Implicit[Option[Default[H]]],
    tail: Lazy[Aux[T, DT, NT, VT, MT, HT, RT, PT]]
   ): Aux[FieldType[K, H] :: T, Option[H] :: DT, List[Name] :: NT, Option[ValueDescription] :: VT, Option[HelpMessage] :: MT, Option[Hidden] :: HT, None.type :: RT, Option[H] :: PT] =
    instance { (default0, names, valueDescriptions, helpMessages, noHelp) =>
      val tailParser = tail.value(default0.tail, names.tail, valueDescriptions.tail, helpMessages.tail, noHelp.tail)
      val headNames = Name(name.value.name) :: names.head
      val headDescriptions = valueDescriptions.head
      val headDefault0 = default0.head

      new Parser[FieldType[K, H] :: T] {
        val args =
          Arg(name.value.name, headNames, headDescriptions, helpMessages.head, noHelp.head.nonEmpty, argParser.value.isFlag) +: tailParser.args

        type D = Option[H] :: PT
        def init = None :: tailParser.init
        def step(args: Seq[String], d: Option[H] :: PT) = {
          if (args.isEmpty)
            Right(None)
          else {
            val matchedOpt = headNames.iterator.map(_.apply(args.head)).collectFirst {
              case Right(valueOpt) => valueOpt
            }

            matchedOpt match {
              case Some(valueOpt) =>
                if (valueOpt.isEmpty && args.tail.isEmpty)
                  argParser.value(d.head).right.map(h => Some((Some(h) :: d.tail, args.tail)))
                else
                  argParser.value(d.head, valueOpt.getOrElse(args.tail.head), valueOpt.nonEmpty).right.flatMap {
                    case (usedArg, h) =>
                      if (valueOpt.nonEmpty && !usedArg)
                        Left(s"Unrecognized value: ${valueOpt.get}")
                      else
                        Right(Some((Some(h) :: d.tail, if (valueOpt.nonEmpty) args.tail else if (usedArg) args.tail.tail else args.tail)))
                  }

              case None =>
                tailParser.step(args, d.tail).right.map(_.map {
                  case (t, args) => (d.head :: t, args)
                })
            }
          }
        }
        def get(d: Option[H] :: PT) = {
          val maybeHead = d.head
            .orElse(headDefault0)
            .orElse(headDefault.value.map(_()))
            .toRight(s"Required option ${name.value.name} / $headNames not specified")
          for {
            h <- maybeHead.right
            t <- tailParser.get(d.tail).right
          } yield field[K](h) :: t
        }
      }
    }

  implicit def hconsRecursive[K <: Symbol, H, HD, T <: HList, PT <: HList, DT <: HList, NT <: HList, VT <: HList, MT <: HList, HT <: HList, RT <: HList]
   (implicit
     headParser: Lazy[Parser.Aux[H, HD]],
     tail: Aux[T, DT, NT, VT, MT, HT, RT, PT]
   ): Aux[FieldType[K, H] :: T, Option[H] :: DT, Nil.type :: NT, None.type :: VT, None.type :: MT, None.type :: HT, Some[Recurse] :: RT, HD :: PT] =
    instance { (default0, names, valueDescriptions, helpMessages, noHelp) =>
      val tailParser = tail(default0.tail, names.tail, valueDescriptions.tail, helpMessages.tail, noHelp.tail)

      new Parser[FieldType[K, H] :: T] {
        val args = headParser.value.args ++ tailParser.args
        type D = HD :: PT
        def init = headParser.value.init :: tailParser.init
        def step(args: Seq[String], d: HD :: PT) =
          headParser.value.step(args, d.head).right.flatMap {
            case None =>
              tailParser.step(args, d.tail).right.map(_.map {
                case (t, args) => (d.head :: t, args)
              })
            case Some((h, args)) =>
              Right(Some(h :: d.tail, args))
          }
        def get(d: HD :: PT) =
          for {
            h <- headParser.value.get(d.head).right
            t <- tailParser.get(d.tail).right
          } yield field[K](h) :: t
      }
    }
}


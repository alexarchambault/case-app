package caseapp.core.parser

import caseapp.core.Arg
import caseapp.{@@, HelpMessage, Hidden, Name, Recurse, ValueDescription}
import caseapp.core.argparser.ArgParser
import caseapp.core.default.Default
import shapeless.{HList, HNil, Strict, Witness, ::}
import shapeless.labelled.{FieldType, field}

sealed abstract class HListParserBuilder[
   L <: HList,
   D <: HList,
  -N <: HList,
  -V <: HList,
  -M <: HList,
  -H <: HList,
   R <: HList
] {
  type P <: HList
  def apply(default: => D, names: N, valueDescriptions: V, helpMessages: M, noHelp: H): Parser.Aux[L, P]
}

object HListParserBuilder extends LowPriorityHListParserBuilder {

  def apply[
    L <: HList,
    D <: HList,
    N <: HList,
    V <: HList,
    M <: HList,
    H <: HList,
    R <: HList
  ](implicit
    args: HListParserBuilder[L, D, N, V, M, H, R]
  ): Aux[L, D, N, V, M, H, R, args.P] =
    args

  type Aux[L <: HList, D <: HList, N <: HList, V <: HList, M <: HList, H <: HList, R <: HList, P0 <: HList] =
    HListParserBuilder[L, D, N, V, M, H, R] { type P = P0 }

  def instance[
    L <: HList,
    D <: HList,
    N <: HList,
    V <: HList,
    M <: HList,
    H <: HList,
    R <: HList,
    P0 <: HList
  ](
    p: (() => D, N, V, M, H) => Parser.Aux[L, P0]
  ): Aux[L, D, N, V, M, H, R, P0] =
    new HListParserBuilder[L, D, N, V, M, H, R] {
      type P = P0
      def apply(default: => D, names: N, valueDescriptions: V, helpMessages: M, noHelp: H) =
        p(() => default, names, valueDescriptions, helpMessages, noHelp)
    }

  implicit val hnil: Aux[HNil, HNil, HNil, HNil, HNil, HNil, HNil, HNil] =
    instance { (_, _, _, _, _) =>
      NilParser
    }

  implicit def hconsTagged[
    K <: Symbol,
    Tag,
    H,
    T <: HList,
    PT <: HList,
    DT <: HList,
    NT <: HList,
    VT <: HList,
    MT <: HList,
    HT <: HList,
    RT <: HList
  ](implicit
    name: Witness.Aux[K],
    argParser: Strict[ArgParser[H @@ Tag]],
    default: Strict[Default[H @@ Tag]],
    tail: Strict[Aux[T, DT, NT, VT, MT, HT, RT, PT]]
   ): Aux[
    FieldType[K, H @@ Tag] :: T,
    Option[H @@ Tag] :: DT,
    List[Name] :: NT,
    Option[ValueDescription] :: VT,
    Option[HelpMessage] :: MT,
    Option[Hidden] :: HT,
    None.type :: RT,
    Option[H @@ Tag] :: PT
  ] =
    hcons[K, H @@ Tag, T, PT, DT, NT, VT, MT, HT, RT]

  implicit def hcons[
    K <: Symbol,
    H,
    T <: HList,
    PT <: HList,
    DT <: HList,
    NT <: HList,
    VT <: HList,
    MT <: HList,
    HT <: HList,
    RT <: HList
  ](implicit
    name: Witness.Aux[K],
    argParser: Strict[ArgParser[H]],
    default: Strict[Default[H]],
    tail: Strict[Aux[T, DT, NT, VT, MT, HT, RT, PT]]
   ): Aux[
    FieldType[K, H] :: T,
    Option[H] :: DT,
    List[Name] :: NT,
    Option[ValueDescription] :: VT,
    Option[HelpMessage] :: MT,
    Option[Hidden] :: HT,
    None.type :: RT,
    Option[H] :: PT
  ] =
    instance { (default0, names, valueDescriptions, helpMessages, noHelp) =>

      val tailParser = tail.value(default0().tail, names.tail, valueDescriptions.tail, helpMessages.tail, noHelp.tail)

      val arg = Arg(
        Name(name.value.name),
        names.head,
        valueDescriptions.head.orElse(Some(new ValueDescription(argParser.value.description))),
        helpMessages.head,
        noHelp.head.nonEmpty,
        argParser.value.isFlag
      )

      ConsParser(arg, argParser.value, () => default0().head.orElse(Some(default.value.value)), tailParser)
        .mapHead(field[K](_))
    }

  implicit def hconsRecursive[
     K <: Symbol,
     H,
     T <: HList,
    PT <: HList,
    DT <: HList,
    NT <: HList,
    VT <: HList,
    MT <: HList,
    HT <: HList,
    RT <: HList
  ](implicit
     headParser: Strict[Parser[H]],
     tail: Aux[T, DT, NT, VT, MT, HT, RT, PT]
   ): Aux[
    FieldType[K, H] :: T,
    Option[H] :: DT,
    Nil.type :: NT,
    None.type :: VT,
    None.type :: MT,
    None.type :: HT,
    Some[Recurse] :: RT,
    headParser.value.D :: PT
  ] =
    instance { (default0, names, valueDescriptions, helpMessages, noHelp) =>

      val tailParser = tail(default0().tail, names.tail, valueDescriptions.tail, helpMessages.tail, noHelp.tail)

      RecursiveConsParser(headParser.value, tailParser)
        .mapHead(field[K](_))
    }
}


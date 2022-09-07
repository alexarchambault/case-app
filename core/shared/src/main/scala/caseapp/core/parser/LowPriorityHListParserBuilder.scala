package caseapp.core.parser

import caseapp._
import caseapp.core.Arg
import caseapp.core.argparser.ArgParser
import caseapp.core.parser.HListParserBuilder.{Aux, instance}
import shapeless.{::, HList, Strict, Witness}
import shapeless.labelled.{FieldType, field}

abstract class LowPriorityHListParserBuilder {

  @deprecated("Redundant with hconsNoDefault", "2.0.7")
  def hconsTaggedNoDefault[
    K <: Symbol,
    Tag,
    H,
    T <: HList,
    PT <: HList,
    DT <: HList,
    NT <: HList,
    VT <: HList,
    MT <: HList,
    GT <: HList,
    HT <: HList,
    TT <: HList,
    RT <: HList
  ](implicit
    name: Witness.Aux[K],
    argParser: Strict[ArgParser[H @@ Tag]],
    tail: Strict[Aux[T, DT, NT, VT, MT, GT, HT, TT, RT, PT]]
  ): Aux[
    FieldType[K, H @@ Tag] :: T,
    Option[H @@ Tag] :: DT,
    List[Name] :: NT,
    Option[ValueDescription] :: VT,
    Option[HelpMessage] :: MT,
    Option[Group] :: GT,
    Option[Hidden] :: HT,
    List[caseapp.Tag] :: TT,
    None.type :: RT,
    Option[H @@ Tag] :: PT
  ] =
    hconsNoDefault[K, H @@ Tag, T, PT, DT, NT, VT, MT, GT, HT, TT, RT]

  implicit def hconsNoDefault[
    K <: Symbol,
    H,
    T <: HList,
    PT <: HList,
    DT <: HList,
    NT <: HList,
    VT <: HList,
    MT <: HList,
    GT <: HList,
    HT <: HList,
    TT <: HList,
    RT <: HList
  ](implicit
    name: Witness.Aux[K],
    argParser: Strict[ArgParser[H]],
    tail: Strict[Aux[T, DT, NT, VT, MT, GT, HT, TT, RT, PT]]
  ): Aux[
    FieldType[K, H] :: T,
    Option[H] :: DT,
    List[Name] :: NT,
    Option[ValueDescription] :: VT,
    Option[HelpMessage] :: MT,
    Option[Group] :: GT,
    Option[Hidden] :: HT,
    List[caseapp.Tag] :: TT,
    None.type :: RT,
    Option[H] :: PT
  ] =
    instance { (default0, names, valueDescriptions, helpMessages, groups, noHelp, tags) =>

      val tailParser = tail.value(
        default0().tail,
        names.tail,
        valueDescriptions.tail,
        helpMessages.tail,
        groups.tail,
        noHelp.tail,
        tags.tail
      )

      val arg = Arg(
        Name(name.value.name),
        names.head,
        valueDescriptions.head.orElse(Some(new ValueDescription(argParser.value.description))),
        helpMessages.head,
        noHelp.head.nonEmpty,
        argParser.value.isFlag,
        groups.head
      ).withTags(tags.head)

      val argument = Argument(arg, argParser.value, () => default0().head)

      ConsParser(argument, tailParser)
        .mapHead(field[K](_))
    }

}

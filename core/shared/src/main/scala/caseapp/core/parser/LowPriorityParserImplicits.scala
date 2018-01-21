package caseapp.core.parser

import caseapp.{HelpMessage, Hidden, Name, Recurse, ValueDescription}
import caseapp.util.AnnotationList
import shapeless.{Annotations, HList, LabelledGeneric, Strict}

abstract class LowPriorityParserImplicits {

  implicit def generic[
    CC,
    L <: HList,
    D <: HList,
    N <: HList,
    V <: HList,
    M <: HList,
    H <: HList,
    R <: HList,
    P <: HList
  ](implicit
    lowPriority: caseapp.util.LowPriority,
    gen: LabelledGeneric.Aux[CC, L],
    defaults: shapeless.Default.AsOptions.Aux[CC, D],
    names: AnnotationList.Aux[Name, CC, N],
    valuesDesc: Annotations.Aux[ValueDescription, CC, V],
    helpMessages: Annotations.Aux[HelpMessage, CC, M],
    noHelp: Annotations.Aux[Hidden, CC, H],
    recurse: Annotations.Aux[Recurse, CC, R],
    parser: Strict[HListParserBuilder.Aux[L, D, N, V, M, H, R, P]]
  ): Parser.Aux[CC, P] =
    parser
      .value
      .apply(defaults(), names(), valuesDesc(), helpMessages(), noHelp())
      .map(gen.from)

}

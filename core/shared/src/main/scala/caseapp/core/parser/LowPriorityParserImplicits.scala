package caseapp.core.parser

import caseapp.{Group, HelpMessage, Hidden, Name, Recurse, ValueDescription}
import caseapp.util.AnnotationList
import shapeless.{Annotations, HList, LabelledGeneric, Strict, Typeable}

abstract class LowPriorityParserImplicits {

  def derive[
    CC,
    L <: HList,
    D <: HList,
    N <: HList,
    V <: HList,
    M <: HList,
    G <: HList,
    H <: HList,
    T <: HList,
    R <: HList,
    P <: HList
  ](implicit
    gen: LabelledGeneric.Aux[CC, L],
    typeable: Typeable[CC],
    defaults: caseapp.util.Default.AsOptions.Aux[CC, D],
    names: AnnotationList.Aux[Name, CC, N],
    valuesDesc: Annotations.Aux[ValueDescription, CC, V],
    helpMessages: Annotations.Aux[HelpMessage, CC, M],
    group: Annotations.Aux[Group, CC, G],
    noHelp: Annotations.Aux[Hidden, CC, H],
    tags: AnnotationList.Aux[caseapp.Tag, CC, T],
    recurse: Annotations.Aux[Recurse, CC, R],
    parser: Strict[HListParserBuilder.Aux[L, D, N, V, M, G, H, T, R, P]]
  ): Parser.Aux[CC, P] =
    parser
      .value
      .apply(
        defaults(),
        names(),
        valuesDesc(),
        helpMessages(),
        group(),
        noHelp(),
        tags()
      )
      .map(gen.from)
      .withDefaultOrigin(typeable.describe)

  implicit def generic[
    CC,
    L <: HList,
    D <: HList,
    N <: HList,
    V <: HList,
    M <: HList,
    G <: HList,
    H <: HList,
    T <: HList,
    R <: HList,
    P <: HList
  ](implicit
    lowPriority: caseapp.util.LowPriority,
    gen: LabelledGeneric.Aux[CC, L],
    typeable: Typeable[CC],
    defaults: caseapp.util.Default.AsOptions.Aux[CC, D],
    names: AnnotationList.Aux[Name, CC, N],
    valuesDesc: Annotations.Aux[ValueDescription, CC, V],
    helpMessages: Annotations.Aux[HelpMessage, CC, M],
    group: Annotations.Aux[Group, CC, G],
    noHelp: Annotations.Aux[Hidden, CC, H],
    tags: AnnotationList.Aux[caseapp.Tag, CC, T],
    recurse: Annotations.Aux[Recurse, CC, R],
    parser: Strict[HListParserBuilder.Aux[L, D, N, V, M, G, H, T, R, P]]
  ): Parser.Aux[CC, P] =
    derive[CC, L, D, N, V, M, G, H, T, R, P](
      gen,
      typeable,
      defaults,
      names,
      valuesDesc,
      helpMessages,
      group,
      noHelp,
      tags,
      recurse,
      parser
    )

}

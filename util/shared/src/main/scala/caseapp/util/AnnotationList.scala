package caseapp.util

import scala.language.experimental.macros
import shapeless.{HList, DepFn0}

sealed abstract class AnnotationList[A, T] extends DepFn0 with Serializable {
  type Out <: HList
}

object AnnotationList {
  def apply[A, T](implicit annotations: AnnotationList[A, T]): Aux[A, T, annotations.Out] =
    annotations

  type Aux[A, T, Out0 <: HList] = AnnotationList[A, T] { type Out = Out0 }

  def instance[A, T, Out0 <: HList](annotations: => Out0): Aux[A, T, Out0] =
    new AnnotationList[A, T] {
      type Out = Out0
      def apply() = annotations
    }

  implicit def materialize[A, T, Out <: HList]: Aux[A, T, Out] =
    macro AnnotationListMacros.materializeAnnotationList[A, T, Out]
}

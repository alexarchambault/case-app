package caseapp.util

import shapeless.Annotation

sealed abstract class AnnotationOption[A, T] extends Serializable {
  def apply(): Option[A]
}

abstract class LowPriorityAnnotationOption {
  implicit def annotationNotFound[A, T]: AnnotationOption[A, T] =
    new AnnotationOption[A, T] {
      def apply() = None
    }
}

object AnnotationOption extends LowPriorityAnnotationOption {
  def apply[A, T](implicit annOpt: AnnotationOption[A, T]): AnnotationOption[A, T] = annOpt

  implicit def annotationFound[A, T](implicit
    annotation: Annotation[A, T]
  ): AnnotationOption[A, T] =
    new AnnotationOption[A, T] {
      def apply() = Some(annotation())
    }
}

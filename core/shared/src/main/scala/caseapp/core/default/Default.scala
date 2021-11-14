package caseapp.core.default

import caseapp.{@@, Tag}
import caseapp.core.Counter

/** Default value for type `T`
  *
  * Allows to give fields of type `T` a default value, even if none was explicitly specified.
  */
final case class Default[T](value: T) extends AnyVal

object Default {

  implicit def option[T]: Default[Option[T]] =
    Default(None)

  implicit def list[T]: Default[List[T]] =
    Default(Nil)

  implicit def vector[T]: Default[Vector[T]] =
    Default(Vector.empty)

  implicit val counter: Default[Int @@ Counter] =
    Default(Tag.of(0))

}

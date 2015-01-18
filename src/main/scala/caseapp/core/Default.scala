package caseapp
package core

import reflect.runtime.universe.{ Try => _, _ }

/**
 * Type class providing a default value for type `CC`
 */
trait Default[CC] {
  def apply(): CC
}

object Default {
  def apply[CC](implicit default: Default[CC]): Default[CC] = default
  
  def from[CC](default: => CC): Default[CC] = new Default[CC] {
    def apply() = default
  }

  // FIXME This should use macros instead of reflection
  implicit def default[CC <: Product : TypeTag]: Default[CC] =
    Default.from(util.instantiateCCWithDefaultValues[CC])

  implicit val unitDefault: Default[Unit] = Default.from(())
  implicit val intDefault: Default[Int] = Default.from(0)
  implicit val counterDefault: Default[Int @@ Counter] = Default.from(Tag of 0)
  implicit val stringDefault: Default[String] = Default.from("")

  implicit def optionDefault[T]: Default[Option[T]] = Default.from(None)
  implicit def listDefault[T]: Default[List[T]] = Default.from(Nil)

}

package caseapp
package internals

import scala.reflect.runtime.universe.{ Try => _, _ }
import scalaz.Tag

trait Default[CC] {
  def apply(): CC
}

object Default {
  
  def apply[CC](default: => CC): Default[CC] = new Default[CC] {
    def apply() = default
  }

  implicit def default[CC <: Product : TypeTag]: Default[CC] =
    Default(util.instantiateCCWithDefaultValues[CC])

  implicit val unitDefault: Default[Unit] = Default(())
  implicit val intDefault: Default[Int] = Default(0)
  implicit val stringDefault: Default[String] = Default("")

  implicit val intCounterDefault: Default[Int @@ Counter] = Default(Tag.of(0))

  implicit def optionDefault[T]: Default[Option[T]] = Default(None)
  implicit def listDefault[T]: Default[List[T]] = Default(Nil)

}

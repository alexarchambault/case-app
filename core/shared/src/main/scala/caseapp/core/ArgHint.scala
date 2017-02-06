package caseapp
package core

trait ArgHint[A] {
  def description: String
  def isRequired: Boolean
}

object ArgHint extends PlatformArgHints {
  def hint[A](desc: String, required: Boolean = true): ArgHint[A] = {
    new ArgHint[A] {
      override def description: String = desc
      override def isRequired: Boolean = required
    }
  }

  def apply[A](implicit ah: ArgHint[A]): ArgHint[A] = ah

  implicit def int: ArgHint[Int] = hint("int")
  implicit def long: ArgHint[Long] = hint("long")
  implicit def double: ArgHint[Double] = hint("double")
  implicit def float: ArgHint[Float] = hint("float")
  implicit def bigDecimal: ArgHint[BigDecimal] = hint("decimal")
  implicit def string: ArgHint[String] = hint("string")
  implicit def unit: ArgHint[Unit] = hint("flag/unit")
  implicit def boolean: ArgHint[Boolean] = hint("bool")
  // FIXME Relevant to note caseapp.core.Messages.scala:61
  // (counters are treated as flags, so this will never be printed currently)
  implicit def counter: ArgHint[Int @@ Counter] = hint("int/counter")
  implicit def list[H: ArgHint]: ArgHint[List[H]] = hint(s"${ArgHint[H].description}*")
  implicit def option[H: ArgHint]: ArgHint[Option[H]] = hint(s"${ArgHint[H].description}?")
}

package caseapp.core.util

import scala.language.implicitConversions

import caseapp.Name

class NameOps(val name: Name) {

  private def isShort: Boolean =
    name.name.length == 1

  private def optionEq(optionFormatter: OptionFormatter): String = 
    option(optionFormatter) + "="

  def option(optionFormatter: OptionFormatter): String =
    if (isShort) s"-${name.name}" else s"--${optionFormatter.format(name)}"

  def apply(
      args: List[String],
      isFlag: Boolean,
      formatter: OptionFormatter
  ): Option[List[String]] =
    args match {
      case Nil => None
      case h :: t =>
        if (h == option(formatter))
          Some(t)
        else if (!isFlag && h.startsWith(optionEq(formatter)))
          Some(h.drop(optionEq(formatter).length) :: t)
        else
          None
    }

  def apply(arg: String, formatter: OptionFormatter): Either[Unit, Option[String]] =
    if (arg == option(formatter))
      Right(None)
    else if (arg.startsWith(optionEq(formatter)))
      Right(Some(arg.drop(optionEq(formatter).length)))
    else
      Left(())

}

object NameOps {
  implicit def toNameOps(name: Name): NameOps =
    new NameOps(name)
}

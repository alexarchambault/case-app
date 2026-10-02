package caseapp.core.util

import scala.language.implicitConversions

import caseapp.Name

class NameOps(val name: Name) extends AnyVal {

  private def isShort: Boolean =
    name.name.length == 1

  private def optionEq(nameFormatter: Formatter[Name]): String =
    option(nameFormatter) + "="

  def option(nameFormatter: Formatter[Name]): String =
    if (name.name.startsWith("-")) nameFormatter.format(name)
    else if (isShort) s"-${name.name}"
    else s"--${nameFormatter.format(name)}"

  def apply(
    args: List[String],
    isFlag: Boolean,
    formatter: Formatter[Name]
  ): Option[List[String]] =
    args match {
      case Nil    => None
      case h :: t =>
        if (h == option(formatter))
          Some(t)
        else if (!isFlag && h.startsWith(optionEq(formatter)))
          Some(h.drop(optionEq(formatter).length) :: t)
        else
          None
    }

  def apply(arg: String, formatter: Formatter[Name]): Either[Unit, Option[String]] =
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

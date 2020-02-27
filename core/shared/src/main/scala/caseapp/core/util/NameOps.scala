package caseapp.core.util

import java.util.Locale

import scala.language.implicitConversions

import caseapp.Name

class NameOps(val name: Name) extends AnyVal {

  private def isShort: Boolean =
    name.name.length == 1

  private def optionName: String =
    CaseUtil.pascalCaseSplit(name.name.toList)
      .map(_.toLowerCase(Locale.ROOT))
      .mkString("-")
  private def optionEq: String =
    if (isShort) s"-${name.name}=" else s"--$optionName="

  def option: String =
    if (isShort) s"-${name.name}" else s"--$optionName"

  def apply(args: List[String], isFlag: Boolean): Option[List[String]] =
    args match {
      case Nil => None
      case h :: t =>
        if (h == option)
          Some(t)
        else if (!isFlag && h.startsWith(optionEq))
          Some(h.drop(optionEq.length) :: t)
        else
          None
    }

  def apply(arg: String): Either[Unit, Option[String]] =
    if (arg == option)
      Right(None)
    else if (arg.startsWith(optionEq))
      Right(Some(arg.drop(optionEq.length)))
    else
      Left(())

}

object NameOps {
  implicit def toNameOps(name: Name): NameOps =
    new NameOps(name)
}

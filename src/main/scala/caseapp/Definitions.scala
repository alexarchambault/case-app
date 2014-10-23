package caseapp

import scala.annotation.meta.getter
import internals.util._

@getter case class Name(name: String) extends annotation.StaticAnnotation {
  private def isShort = name.length == 1

  val optionName = pascalCaseSplit(name.toList).map(_.toLowerCase).mkString("-")
  def option = opt
  private val opt = if (isShort) s"-$name" else s"--$optionName"
  private val optEq = if (isShort) s"-$name=" else s"--$optionName="

  def apply(args: List[String], isFlag: Boolean): Option[List[String]] = args match {
    case Nil => None
    case h :: t =>
      if (h == opt)
        Some(t)
      else if (!isFlag && h.startsWith(optEq))
        Some(h.drop(optEq.length) :: t)
      else
        None
  }
}

@getter case class ValueDescription(description: String) extends annotation.StaticAnnotation {
  def message: String = s"<$description>"
}
@getter case class HelpMessage(message: String) extends annotation.StaticAnnotation

sealed trait Counter

package caseapp

import annotation.meta.getter
import core.util._

/**
 * Provides an extra name for the annotated argument
 */
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

/**
 * Provides a description of the value of the annotated argument
 */
@getter case class ValueDescription(description: String) extends annotation.StaticAnnotation {
  def message: String = s"<$description>"
}

/**
 * Provides a help message for the annotated argument
 */
@getter case class HelpMessage(message: String) extends annotation.StaticAnnotation

/**
 * Provides a name for the annotated case class of arguments
 */
case class AppName(appName: String) extends annotation.StaticAnnotation {
  private[caseapp] def orElse(other: AppName): AppName = AppName(
    if (appName.nonEmpty) appName else other.appName
  )
}

/**
 * Provides a program name for the annotated case class of arguments
 */
case class ProgName(progName: String) extends annotation.StaticAnnotation {
  private[caseapp] def orElse(other: ProgName): ProgName = ProgName(
    if (progName.nonEmpty) progName else other.progName
  )
}

/**
 * Provides the app version for the annotated case class of arguments
 */
case class AppVersion(appVersion: String) extends annotation.StaticAnnotation {
  private[caseapp] def orElse(other: AppVersion): AppVersion = AppVersion(
    if (appVersion.nonEmpty) appVersion else other.appVersion
  )
}

/**
 * Provides a name for the extra arguments of the annotated case class of arguments
 */
case class ArgsName(argsName: String) extends annotation.StaticAnnotation {
  private[caseapp] def orElse(other: ArgsName): ArgsName = ArgsName(
    if (argsName.nonEmpty) argsName else other.argsName
  )
}

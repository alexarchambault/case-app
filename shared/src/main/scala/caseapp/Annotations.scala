package caseapp

import core.util._
import scala.annotation.StaticAnnotation

/**
 * Extra name for the annotated argument
 */
case class Name(name: String) extends StaticAnnotation {
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

  def apply(arg: String): Either[Unit, Option[String]] =
    if (arg == opt)
      Right(None)
    else if (arg.startsWith(optEq))
      Right(Some(arg.drop(optEq.length)))
    else
      Left(())
}

/**
 * Description of the value of the annotated argument
 */
case class ValueDescription(description: String) extends StaticAnnotation {
  def message: String = s"<$description>"
}

/**
 * Help message for the annotated argument
 */
case class HelpMessage(message: String) extends StaticAnnotation

/**
 * Name for the annotated case class of arguments
 * E.g. MyApp
 */
case class AppName(appName: String) extends StaticAnnotation

/**
 * Program name for the annotated case class of arguments
 * E.g. my-app
 */
case class ProgName(progName: String) extends StaticAnnotation

/**
  * Set the command name of the annotated case class of arguments
  * E.g. my-app
  */
case class CommandName(commandName: String) extends StaticAnnotation

/**
 * App version for the annotated case class of arguments
 */
case class AppVersion(appVersion: String) extends StaticAnnotation

/**
 * Name for the extra arguments of the annotated case class of arguments
 */
case class ArgsName(argsName: String) extends StaticAnnotation

class Recurse extends StaticAnnotation

/**
 * Do not include this field / argument in the help message
 */
class Hidden extends StaticAnnotation
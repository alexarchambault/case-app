package caseapp

import scala.annotation.StaticAnnotation

/** Extra name for the annotated argument
  */
final case class Name(name: String) extends StaticAnnotation

/** Description of the value of the annotated argument
  */
final case class ValueDescription(description: String) extends StaticAnnotation {
  def message: String = s"<$description>"
}

object ValueDescription {
  val default = ValueDescription("value")
}

/** Help message for the annotated argument
  * @messageMd
  *   not used by case-app itself, only there as a convenience for case-app users
  */
final case class HelpMessage(message: String, messageMd: String = "") extends StaticAnnotation

/** Name for the annotated case class of arguments E.g. MyApp
  */
final case class AppName(appName: String) extends StaticAnnotation

/** Program name for the annotated case class of arguments E.g. my-app
  */
final case class ProgName(progName: String) extends StaticAnnotation

/** Set the command name of the annotated case class of arguments E.g. my-app
  */
final case class CommandName(commandName: String) extends StaticAnnotation

/** App version for the annotated case class of arguments
  */
final case class AppVersion(appVersion: String) extends StaticAnnotation

/** Name for the extra arguments of the annotated case class of arguments
  */
final case class ArgsName(argsName: String) extends StaticAnnotation

/** Don't parse the annotated field as a single argument. Recurse on its fields instead.
  */
final class Recurse extends StaticAnnotation

/** Do not include this field / argument in the help message
  */
final class Hidden extends StaticAnnotation

final case class Group(name: String) extends StaticAnnotation

final case class Tag(name: String) extends StaticAnnotation

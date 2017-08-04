package caseapp

import scala.annotation.StaticAnnotation

/**
  * Extra name for the annotated argument
  */
final case class Name(name: String) extends StaticAnnotation

/**
  * Description of the value of the annotated argument
  */
final class ValueDescription(val description: String) extends StaticAnnotation {
  def message: String = s"<$description>"
}

object ValueDescription {
  val default = new ValueDescription("value")
}

/**
  * Help message for the annotated argument
  */
final class HelpMessage(val message: String) extends StaticAnnotation

/**
  * Name for the annotated case class of arguments
  * E.g. MyApp
  */
final class AppName(val appName: String) extends StaticAnnotation

/**
  * Program name for the annotated case class of arguments
  * E.g. my-app
  */
final class ProgName(val progName: String) extends StaticAnnotation

/**
  * Set the command name of the annotated case class of arguments
  * E.g. my-app
  */
final class CommandName(val commandName: String) extends StaticAnnotation

/**
  * App version for the annotated case class of arguments
  */
final class AppVersion(val appVersion: String) extends StaticAnnotation

/**
  * Name for the extra arguments of the annotated case class of arguments
  */
final class ArgsName(val argsName: String) extends StaticAnnotation

/**
  * Don't parse the annotated field as a single argument. Recurse on its fields instead.
  */
final class Recurse extends StaticAnnotation

/**
  * Do not include this field / argument in the help message
  */
final class Hidden extends StaticAnnotation
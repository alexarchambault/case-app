package caseapp

import scala.annotation.StaticAnnotation

/**
 * Extra name for the annotated argument
 */
case class Name(name: String) extends StaticAnnotation

/**
 * Description of the value of the annotated argument
 */
case class ValueDescription(description: String) extends StaticAnnotation

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
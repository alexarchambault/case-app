package caseapp.core.util

import caseapp.{Name, Recurse}

abstract class Formatter[T] {
  def format(t: T): String
}

object Formatter {

  /** Formats option arguments to a given format.
    *
    * Default formatter will format option arguments as `foo-bar`.
    */
  val DefaultNameFormatter: Formatter[Name] = new Formatter[Name] {
    override def format(name: Name): String =
      CaseUtil
        .pascalCaseSplit(name.name.toList)
        .map(_.toLowerCase)
        .mkString("-")
  }

  /** Adds the prefix for the Recurse annotation to the names formatted by the formatter.
    *
    * Adds the prefix as `prefix-foo-bar`.
    */
  def addRecursePrefix(recurse: Recurse, formatter: Formatter[Name]): Formatter[Name] =
    if (recurse.prefix.isEmpty()) formatter
    else {
      new Formatter[Name] {
        def format(t: Name): String = {
          val formattedPrefix = formatter.format(Name(recurse.prefix))
          s"${formattedPrefix}-${formatter.format(t)}"
        }
      }
    }
}

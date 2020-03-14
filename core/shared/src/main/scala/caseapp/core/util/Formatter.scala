package caseapp.core.util

import caseapp.Name

abstract class Formatter[T] {
  def format(t: T): String
}

object Formatter {

  /**
    * Formats option arguments to a given format.
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
}

package caseapp.core.util

import caseapp.Name

abstract class OptionFormatter {
  def format(name: Name): String
}

object OptionFormatter {
  val DefaultFormatter = new OptionFormatter {
    override def format(name: Name): String =
      CaseUtil
        .pascalCaseSplit(name.name.toList)
        .map(_.toLowerCase)
        .mkString("-")
  }
}

package caseapp.core.help

import caseapp.core.util.fansi
import dataclass._

@data class HelpFormat(
  progName: fansi.Attrs = fansi.Attrs.Empty,
  option: fansi.Attrs = fansi.Attrs.Empty,
  newLine: String = System.lineSeparator(),
  terminalWidth: Int = 80,
  sortGroups: Option[Seq[String] => Seq[String]] = None,
  sortedGroups: Option[Seq[String]] = None
) {
  def sortGroupValues[T](elems: Seq[(String, T)]): Seq[(String, T)] =
    sortGroups match {
      case None =>
        sortedGroups match {
          case None =>
            elems.sortBy(_._1)
          case Some(sortedGroups0) =>
            val sorted = sortedGroups0.zipWithIndex.toMap
            elems.sortBy { case (group, _) => sorted.getOrElse(group, Int.MaxValue) }
        }
      case Some(sort) =>
        val sorted = sort(elems.map(_._1)).zipWithIndex.toMap
        elems.sortBy { case (group, _) => sorted.getOrElse(group, Int.MaxValue) }
    }
}

object HelpFormat {
  def default(): HelpFormat =
    default(true)
  def default(ansiColors: Boolean): HelpFormat =
    if (ansiColors)
      HelpFormat()
        .withProgName(fansi.Bold.On)
        .withOption(fansi.Color.Yellow)
    else
      HelpFormat()
}

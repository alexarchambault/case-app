package caseapp.core.help

import caseapp.core.util.fansi
import dataclass._

@data class HelpFormat(
  progName: fansi.Attrs = fansi.Attrs.Empty,
  commandName: fansi.Attrs = fansi.Attrs.Empty,
  option: fansi.Attrs = fansi.Attrs.Empty,
  newLine: String = System.lineSeparator(),
  @deprecated("This field is now ignored, use terminalWidthOpt instead", "2.1.0-M7")
  terminalWidth: Int = 80,
  @since("2.1.0")
  sortGroups: Option[Seq[String] => Seq[String]] = None,
  sortedGroups: Option[Seq[String]] = None,
  hiddenGroups: Option[Seq[String]] = None,
  @since("2.1.0")
  sortCommandGroups: Option[Seq[String] => Seq[String]] = None,
  sortedCommandGroups: Option[Seq[String]] = None,
  @since("2.1.0")
  hidden: fansi.Attrs = fansi.Attrs.Empty,
  @since("2.1.0")
  terminalWidthOpt: Option[Int] = None
) {
  private def sortValues[T](
    sortGroups: Option[Seq[String] => Seq[String]],
    sortedGroups: Option[Seq[String]],
    elems: Seq[(String, T)]
  ): Seq[(String, T)] = {
    val sortedGroups0 = sortGroups match {
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
    sortedGroups0.filter { case (group, _) => hiddenGroups.forall(!_.contains(group)) }
  }

  def sortGroupValues[T](elems: Seq[(String, T)]): Seq[(String, T)] =
    sortValues(sortGroups, sortedGroups, elems)
  def sortCommandGroupValues[T](elems: Seq[(String, T)]): Seq[(String, T)] =
    sortValues(sortCommandGroups, sortedCommandGroups, elems)
}

object HelpFormat {
  def default(): HelpFormat =
    default(true)
  def default(ansiColors: Boolean): HelpFormat =
    if (ansiColors)
      HelpFormat()
        .withProgName(fansi.Bold.On)
        .withCommandName(fansi.Bold.On)
        .withOption(fansi.Color.Yellow)
        .withHidden(fansi.Color.DarkGray)
    else
      HelpFormat()
}

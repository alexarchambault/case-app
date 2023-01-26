package caseapp.core.help

import caseapp.core.Arg
import caseapp.core.Scala3Helpers._
import caseapp.core.util.fansi
import dataclass._

@data case class HelpFormat(
  progName: fansi.Attrs = fansi.Attrs.Empty,
  commandName: fansi.Attrs = fansi.Attrs.Empty,
  option: fansi.Attrs = fansi.Attrs.Empty,
  newLine: String = System.lineSeparator(),
  sortGroups: Option[Seq[String] => Seq[String]] = None,
  sortedGroups: Option[Seq[String]] = None,
  hiddenGroups: Option[Seq[String]] = None,
  sortCommandGroups: Option[Seq[String] => Seq[String]] = None,
  sortedCommandGroups: Option[Seq[String]] = None,
  hidden: fansi.Attrs = fansi.Attrs.Empty,
  terminalWidthOpt: Option[Int] = None,
  @since filterArgs: Option[Arg => Boolean] = None
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

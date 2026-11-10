package caseapp.core.help

import caseapp.core.Arg
import caseapp.core.util.fansi
import dataclass._

/** Format of help messages
  *
  * Controls colors, the order of option and command groups, which options or groups are shown, etc.
  * Usually obtained with [[HelpFormat.default()* HelpFormat.default()]], then customized with
  * `copy`.
  *
  * {{{
  * HelpFormat.default().copy(
  *   sortedGroups = Some(Seq("Main", "Help")),
  *   terminalWidthOpt = Some(80)
  * )
  * }}}
  *
  * Help messages can be shown with or without hidden options: help messages printed with `--help`
  * don't show them, while full help messages printed with `--help-full` show them (see
  * `Help#help`). Some of the fields below only apply to one of these two cases.
  *
  * @param progName
  *   style of the program name, in usage lines
  * @param commandName
  *   style of the command names, in command lists
  * @param option
  *   style of the option names, in option lists
  * @param newLine
  *   line separator
  * @param sortGroups
  *   function sorting option groups (see the `Group` annotation), given their names. Takes
  *   precedence over `sortedGroups`.
  * @param sortedGroups
  *   order of the option groups, groups not in this list come after the others. If neither this nor
  *   `sortGroups` are set, groups are sorted alphabetically. Options with no group always come
  *   last.
  * @param hiddenGroups
  *   option groups not to show, when hidden options aren't shown
  * @param sortCommandGroups
  *   like `sortGroups`, for command groups (see [[caseapp.core.app.Command.group]])
  * @param sortedCommandGroups
  *   like `sortedGroups`, for command groups
  * @param hidden
  *   style of the `(hidden)` marker of hidden options and commands
  * @param terminalWidthOpt
  *   if set, the descriptions of applications are wrapped to this width
  * @param filterArgs
  *   if set, only the options it accepts are shown, when hidden options aren't shown
  * @param filterArgsWhenShowHidden
  *   if set, only the options it accepts are shown, when hidden options are shown
  * @param hiddenGroupsWhenShowHidden
  *   option groups not to show, when hidden options are shown
  * @param namesLimit
  *   if set, maximum number of names shown per option (main name first, then extra names), when
  *   hidden options aren't shown
  */
@data(
  deprecatedSetters = true,
  deprecatedSettersMessage = "Use copy instead",
  deprecatedSettersSince = "2.1.2"
) case class HelpFormat(
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
  @since filterArgs: Option[Arg => Boolean] = None,
  @since filterArgsWhenShowHidden: Option[Arg => Boolean] = None,
  hiddenGroupsWhenShowHidden: Option[Seq[String]] = None,
  @since("2.1.0-M25")
  namesLimit: Option[Int] = None
) {
  private def sortValues[T](
    sortGroups: Option[Seq[String] => Seq[String]],
    sortedGroups: Option[Seq[String]],
    elems: Seq[(String, T)],
    showHidden: Boolean
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
    sortedGroups0.filter { case (group, _) =>
      (if (showHidden) hiddenGroupsWhenShowHidden else hiddenGroups).forall(!_.contains(group))
    }
  }

  /** Sorts and filters option groups, according to `sortGroups`, `sortedGroups`, `hiddenGroups`,
    * and `hiddenGroupsWhenShowHidden`
    *
    * @param elems
    *   group names, along with the corresponding values
    */
  def sortGroupValues[T](elems: Seq[(String, T)], showHidden: Boolean): Seq[(String, T)] =
    sortValues(sortGroups, sortedGroups, elems, showHidden)

  /** Sorts and filters command groups, according to `sortCommandGroups`, `sortedCommandGroups`,
    * `hiddenGroups`, and `hiddenGroupsWhenShowHidden`
    *
    * @param elems
    *   group names, along with the corresponding values
    */
  def sortCommandGroupValues[T](elems: Seq[(String, T)], showHidden: Boolean): Seq[(String, T)] =
    sortValues(sortCommandGroups, sortedCommandGroups, elems, showHidden)
}

object HelpFormat {

  /** Default help format, with ANSI colors */
  def default(): HelpFormat =
    default(true)

  /** Default help format
    *
    * @param ansiColors
    *   whether to use ANSI colors: if `true`, program and command names are in bold, option names
    *   in yellow, and the `(hidden)` markers in dark gray
    */
  def default(ansiColors: Boolean): HelpFormat =
    if (ansiColors)
      HelpFormat()
        .copy(
          progName = fansi.Bold.On,
          commandName = fansi.Bold.On,
          option = fansi.Color.Yellow,
          hidden = fansi.Color.DarkGray
        )
    else
      HelpFormat()
}

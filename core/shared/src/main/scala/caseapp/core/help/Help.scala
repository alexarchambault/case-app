package caseapp.core.help

import caseapp.core.Arg
import caseapp.{Name, ValueDescription}
import caseapp.core.parser.Parser
import caseapp.core.util.fansi
import caseapp.core.util.NameOps.toNameOps
import dataclass._
import caseapp.core.util.Formatter
import caseapp.HelpMessage

/** Provides usage and help messages related to `T`
  *
  * Like [[caseapp.core.parser.Parser]], `Help` instances for case classes are derived
  * automatically, from their parser and the annotations on them (`AppName`, `ProgName`, `ArgsName`,
  * `HelpMessage`…). They can also be derived explicitly with `Help.derive` (or with a
  * `derives Help` clause, on Scala 3).
  *
  * {{{
  * @HelpMessage("Greets people")
  * case class GreetOptions(
  *   @HelpMessage("Name of the person to greet")
  *   name: String = "World"
  * )
  *
  * println(Help[GreetOptions].help)
  * // Usage: greet [options]
  * // Greets people
  * //
  * // Options:
  * //   --name string  Name of the person to greet
  * }}}
  *
  * The help options (`--help`, `--usage`…) aren't part of it, see [[withHelp]] and [[withFullHelp]]
  * to add them.
  *
  * @param args
  *   arguments accepted by `T`, as computed by [[caseapp.core.parser.Parser]]
  * @param appName
  *   application name (see the `AppName` annotation), not printed in help messages
  * @param appVersion
  *   application version (see the `AppVersion` annotation), not printed in help messages
  * @param progName
  *   program name (see the `ProgName` annotation), printed in the usage line
  * @param argsNameOption
  *   name of the non-option arguments (see the `ArgsName` annotation), printed between brackets at
  *   the end of the usage line
  * @param optionsDesc
  *   description of the options, printed in the usage line after [[progName]] if there are options,
  *   `[options]` by default
  * @param nameFormatter
  *   formats the names of the arguments into option names
  * @param helpMessage
  *   description of the application (see the `HelpMessage` annotation), printed after the usage
  *   line
  */
@data(
  deprecatedSetters = true,
  deprecatedSettersMessage = "Use copy instead",
  deprecatedSettersSince = "2.1.2"
) case class Help[T](
  args: Seq[Arg] = Nil,
  appName: String = "",
  appVersion: String = "",
  progName: String = "",
  argsNameOption: Option[String] = None,
  @since
  optionsDesc: String = Help.DefaultOptionsDesc,
  nameFormatter: Formatter[Name] = Help.DefaultNameFormatter,
  helpMessage: Option[HelpMessage] = Help.DefaultHelpMessage
) {

  /** Whether there are some arguments */
  def nonEmpty = args.nonEmpty

  /** One-line usage message for `T`, like `Usage: my-app [options]`
    *
    * Formatted with [[HelpFormat.default()* HelpFormat.default()]], which uses ANSI colors.
    */
  def usage: String =
    usage(HelpFormat.default())

  /** One-line usage message for `T`, formatted with `format` */
  def usage(format: HelpFormat): String = {
    val b = new StringBuilder
    printUsage(b, format)
    b.result()
  }

  /** Options description for `T`, see `Help.optionsMessage`
    */
  def options: String = Help.optionsMessage(args, nameFormatter, showHidden = false)

  /** Full multi-line help message for `T`.
    *
    * Includes the usage line, the description of `T` (if it has a `HelpMessage` annotation), and
    * the options, grouped by `Group`. Hidden options aren't included. Formatted with
    * [[HelpFormat.default()* HelpFormat.default()]], which uses ANSI colors.
    */
  def help: String =
    help(HelpFormat.default(), showHidden = false)

  /** Add help and usage options.
    *
    * Adds the options of [[WithHelp]] (`--usage`, `--help`, `-h`) to [[args]].
    */
  def withHelp: Help[WithHelp[T]] = {
    final case class Dummy()
    implicit val parser: Parser[WithHelp[Dummy]] = WithHelp.parser
    val helpArgs                                 = parser.args

    this.copy(args = helpArgs ++ args)
      // circumventing a possible data-class issue here (getting a Help[Nothing] else)
      .asInstanceOf[Help[WithHelp[T]]]
  }

  /** Add help, usage, and full help options.
    *
    * Adds the options of [[WithFullHelp]] (`--usage`, `--help`, `-h`, `--help-full`…) to [[args]].
    */
  def withFullHelp: Help[WithFullHelp[T]] = {
    final case class Dummy()
    val parser: Parser[WithFullHelp[Dummy]] = WithFullHelp.parser
    val helpArgs                            = parser.args

    this.copy(args = helpArgs ++ args)
      // circumventing a possible data-class issue here (getting a Help[Nothing] else)
      .asInstanceOf[Help[WithFullHelp[T]]]
  }

  /** Arguments sharing an option name, by option name */
  def duplicates: Map[String, Seq[Arg]] = {
    val pairs = args.flatMap(a => a.names.map(n => n.option(nameFormatter) -> a))
    pairs
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .iterator
      .filter(_._2.lengthCompare(1) > 0)
      .toMap
  }

  /** Throws an exception if several arguments share an option name
    *
    * @throws java.lang.Exception
    *   if some option names are duplicated
    */
  def ensureNoDuplicates(): Unit =
    if (duplicates.nonEmpty) {
      val message = duplicates
        .toVector
        .sortBy(_._1)
        .map {
          case (name, count) =>
            s"$name ($count times)"
        }
        .mkString("Found duplicated arguments: ", ", ", ".")
      throw new Exception(message)
    }

  /** Writes the usage line (see [[usage:String* usage]]) to `b` */
  def printUsage(b: StringBuilder, format: HelpFormat): Unit = {
    b.append("Usage: ")
    b.append(format.progName(progName).render)

    if (args.nonEmpty) {
      b.append(" ")
      b.append(optionsDesc)
    }

    for (argName <- argsNameOption) {
      b.append(" [")
      b.append(argName)
      b.append("]")
    }
  }

  /** Writes the help message (see `help`) to `b`
    */
  def printHelp(b: StringBuilder, format: HelpFormat, showHidden: Boolean): Unit = {
    printUsage(b, format)
    b.append(format.newLine)

    val helpDescription = helpMessage.map {
      case HelpMessage(_, _, detailedMessage) if showHidden && detailedMessage.nonEmpty =>
        detailedMessage
      case HelpMessage(message, _, _) => message
    }

    for (desc <- helpDescription)
      Help.printDescription(
        b,
        desc,
        format.newLine,
        format.terminalWidthOpt.getOrElse(Int.MaxValue)
      )

    b.append(format.newLine)

    printOptions(b, format, showHidden)
  }

  /** Multi-line help message for `T`, formatted with `format`, without hidden options */
  def help(format: HelpFormat): String =
    help(format, showHidden = false)

  /** Multi-line help message for `T`, formatted with `format`
    *
    * @param showHidden
    *   whether to include hidden options (see the `Hidden` annotation), and to use the
    *   `detailedMessage` of the `HelpMessage` annotation of `T`, if it's not empty. That's what is
    *   printed with `--help-full`.
    */
  def help(format: HelpFormat, showHidden: Boolean): String = {
    val b = new StringBuilder
    printHelp(b, format, showHidden)
    b.result()
  }

  /** Writes the options table, grouped by `Group`, to `b`
    *
    * Groups are sorted and filtered according to `format`, see [[HelpFormat]].
    */
  def printOptions(b: StringBuilder, format: HelpFormat, showHidden: Boolean): Unit =
    if (args.nonEmpty) {
      val filteredArgs =
        if (showHidden) format.filterArgsWhenShowHidden.map(args.filter).getOrElse(args)
        else format.filterArgs.map(args.filter).getOrElse(args)
      val groupedArgs  = filteredArgs.groupBy(_.group.fold("")(_.name))
      val groups       = format.sortGroupValues(groupedArgs.toVector, showHidden)
      val sortedGroups = groups.filter(_._1.nonEmpty) ++ groupedArgs.get("").toSeq.map("" -> _)
      for {
        ((groupName, groupArgs), groupIdx) <- sortedGroups.zipWithIndex
        argsAndDescriptions = Table(Help.optionsTable(
          groupArgs,
          format,
          nameFormatter,
          showHidden
        ).toVector.map(_.toVector))
        if argsAndDescriptions.lines.nonEmpty
      } {
        if (groupIdx > 0) {
          b.append(format.newLine)
          b.append(format.newLine)
        }
        if (groupName.isEmpty)
          if (groups.length > 1)
            b.append("Other options:")
          else
            b.append("Options:")
        else {
          b.append(groupName)
          b.append(" options:")
        }
        b.append(format.newLine)
        argsAndDescriptions.render(
          b,
          "  ",
          "  ",
          format.newLine,
          argsAndDescriptions.widths.map(_.min(45)).toVector
        )
      }
    }
}

object Help extends HelpCompanion {

  /** Default value of [[Help.optionsDesc]], `[options]` */
  val DefaultOptionsDesc = "[options]"

  /** Default value of [[Help.nameFormatter]] */
  val DefaultNameFormatter = Formatter.DefaultNameFormatter

  /** Default value of [[Help.helpMessage]], none */
  val DefaultHelpMessage = Option.empty[HelpMessage]

  /** Look for an implicit `Help[T]` */
  def apply[T](implicit help: Help[T]): Help[T] = help

  /** Option message for `args` */
  def optionsMessage(args: Seq[Arg]): String =
    optionsMessage(args, Formatter.DefaultNameFormatter, showHidden = false)

  /** Option message for `args` */
  def optionsMessage(args: Seq[Arg], nameFormatter: Formatter[Name], showHidden: Boolean): String =
    args
      .collect {
        case arg if showHidden || !arg.noHelp =>
          val names = arg.names.distinct

          // FIXME Flags that accept no value are not given the right help message here
          val valueDescription = arg
            .valueDescription
            .orElse(if (arg.isFlag) None else Some(ValueDescription.default))

          val message = arg.helpMessage.map(Help.TB + _.message)

          val usage =
            s"${Help.WW}${names.map(_.option(nameFormatter)) mkString " | "}  ${valueDescription.map(_.message).mkString}"

          (usage :: message.toList).mkString(Help.NL)
      }
      .mkString(Help.NL)

  // From scopt
  val NL = PlatformUtil.NL
  val WW = "  "
  val TB = "        "

  private def optionsTable(
    args: Seq[Arg],
    format: HelpFormat,
    nameFormatter: Formatter[Name],
    showHidden: Boolean
  ): Seq[Seq[fansi.Str]] =
    for (arg <- args if showHidden || !arg.noHelp) yield {
      val namesToShow = format.namesLimit match {
        case Some(limit) if !showHidden && limit >= 0 =>
          arg.names.take(limit)
        case _ =>
          arg.names
      }

      val sortedNames = namesToShow
        .map(name => format.option(name.option(nameFormatter)))
        .groupBy(_.length)
        .toVector
        .sortBy(_._1)
        .flatMap(_._2)
      val options = sortedNames
        .iterator
        .zip(Iterator.continually(", ": fansi.Str))
        .flatMap { case (a, b) => Iterator(a, b) }
        .toVector
        .take(sortedNames.length * 2 - 1)
        .foldLeft("": fansi.Str)(_ ++ _)

      val optionsAndValue =
        if (arg.isFlag) options
        else options ++ " " ++ arg.valueDescription.fold("value")(_.description)

      val desc: fansi.Str =
        if (arg.noHelp)
          format.hidden("(hidden)") ++
            arg.helpMessage.map(_.message).filter(_.nonEmpty).fold("")(" " + _)
        else arg.helpMessage.fold("")(_.message)

      Seq[fansi.Str](optionsAndValue, desc)
    }

  /** Wraps `input` so that its lines aren't longer than `terminalWidth` */
  def reflowed(input: String, newLine: String, terminalWidth: Int): String =
    if (input.length <= terminalWidth)
      input
    else
      WordUtils.wrap(input, terminalWidth, Some(newLine), wrapLongWords = true, wrapOn = " ")

  /** Writes `desc`, wrapped to `terminalWidth` (see [[reflowed]]), and followed by a new line, to
    * `b`
    */
  def printDescription(
    b: StringBuilder,
    desc: String,
    newLine: String,
    terminalWidth: Int
  ): Unit = {
    val wrappedDesc = reflowed(desc, newLine, terminalWidth)
    b.append(wrappedDesc)
    if (!wrappedDesc.endsWith(newLine))
      b.append(newLine)
  }

}

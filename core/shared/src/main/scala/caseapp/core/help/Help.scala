package caseapp.core.help

import caseapp.core.Arg
import caseapp.{AppName, AppVersion, ArgsName, Name, ProgName, ValueDescription}
import caseapp.core.parser.Parser
import caseapp.core.util.{CaseUtil, fansi}
import caseapp.util.AnnotationOption
import caseapp.core.util.NameOps.toNameOps
import dataclass._
import shapeless.Typeable
import caseapp.core.util.Formatter
import caseapp.HelpMessage

/** Provides usage and help messages related to `T`
  */
@data class Help[T](
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

  def nonEmpty = args.nonEmpty

  /** One-line usage message for `T` */
  def usage: String =
    usage(HelpFormat.default())
  def usage(format: HelpFormat): String = {
    val b = new StringBuilder
    printUsage(b, format)
    b.result()
  }

  /** Options description for `T` */
  def options: String = Help.optionsMessage(args, nameFormatter, showHidden = false)

  /** Full multi-line help message for `T`.
    *
    * Includes both `usageMessage` and `optionsMessage`
    */
  def help: String =
    help(HelpFormat.default(), showHidden = false)

  /** Add help and usage options.
    */
  def withHelp: Help[WithHelp[T]] = {
    final case class Dummy()
    val helpArgs = Parser[WithHelp[Dummy]].args

    withArgs(helpArgs ++ args)
      // circumventing a possible data-class issue here (getting a Help[Nothing] else)
      .asInstanceOf[Help[WithHelp[T]]]
  }

  def withFullHelp: Help[WithFullHelp[T]] = {
    final case class Dummy()
    val helpArgs = Parser[WithFullHelp[Dummy]].args

    withArgs(helpArgs ++ args)
      // circumventing a possible data-class issue here (getting a Help[Nothing] else)
      .asInstanceOf[Help[WithFullHelp[T]]]
  }

  def duplicates: Map[String, Seq[Arg]] = {
    val pairs = args.map(a => a.name.option(nameFormatter) -> a) ++
      args.flatMap(a => a.extraNames.map(n => n.option(nameFormatter) -> a))
    pairs
      .groupBy(_._1)
      .mapValues(_.map(_._2))
      .iterator
      .filter(_._2.lengthCompare(1) > 0)
      .toMap
  }

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

  def printHelp(b: StringBuilder, format: HelpFormat, showHidden: Boolean): Unit = {
    printUsage(b, format)
    b.append(format.newLine)

    for (desc <- helpMessage.map(_.message))
      Help.printDescription(
        b,
        desc,
        format.newLine,
        format.terminalWidthOpt.getOrElse(Int.MaxValue)
      )

    b.append(format.newLine)

    printOptions(b, format, showHidden)
  }

  def help(format: HelpFormat): String =
    help(format, showHidden = false)

  def help(format: HelpFormat, showHidden: Boolean): String = {
    val b = new StringBuilder
    printHelp(b, format, showHidden)
    b.result()
  }

  def printOptions(b: StringBuilder, format: HelpFormat, showHidden: Boolean): Unit =
    if (args.nonEmpty) {
      val groupedArgs  = args.groupBy(_.group.fold("")(_.name))
      val groups       = format.sortGroupValues(groupedArgs.toVector)
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

object Help {
  val DefaultOptionsDesc   = "[options]"
  val DefaultNameFormatter = Formatter.DefaultNameFormatter
  val DefaultHelpMessage   = Option.empty[HelpMessage]

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
          val names = (arg.name +: arg.extraNames).distinct

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

  // FIXME Not sure Typeable is fine on Scala JS, should be replaced by something else

  def derive[T](implicit
    parser: Parser[T],
    typeable: Typeable[T],
    appName: AnnotationOption[AppName, T],
    appVersion: AnnotationOption[AppVersion, T],
    progName: AnnotationOption[ProgName, T],
    argsName: AnnotationOption[ArgsName, T],
    helpMessage: AnnotationOption[HelpMessage, T]
  ): Help[T] =
    help[T](
      parser,
      typeable,
      appName,
      appVersion,
      progName,
      argsName,
      helpMessage
    )

  /** Implicitly derives a `Help[T]` for `T` */
  implicit def help[T](implicit
    parser: Parser[T],
    typeable: Typeable[T],
    appName: AnnotationOption[AppName, T],
    appVersion: AnnotationOption[AppVersion, T],
    progName: AnnotationOption[ProgName, T],
    argsName: AnnotationOption[ArgsName, T],
    helpMessage: AnnotationOption[HelpMessage, T]
  ): Help[T] = {

    val appName0 = appName().fold(typeable.describe.stripSuffix("Options"))(_.appName)

    Help(
      parser.args,
      appName0,
      appVersion().fold("")(_.appVersion),
      progName().fold(CaseUtil.pascalCaseSplit(appName0.toList).map(_.toLowerCase).mkString("-"))(
        _.progName
      ),
      argsName().map(_.argsName),
      Help.DefaultOptionsDesc,
      parser.defaultNameFormatter,
      helpMessage()
    )
  }

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
      val sortedNames = (arg.name +: arg.extraNames)
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

  def reflowed(input: String, newLine: String, terminalWidth: Int): String =
    if (input.length <= terminalWidth)
      input
    else
      WordUtils.wrap(input, terminalWidth, Some(newLine), wrapLongWords = true, wrapOn = " ")

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

package caseapp.core.help

import caseapp.core.Arg
import caseapp.{AppName, AppVersion, ArgsName, Name, ProgName, ValueDescription}
import caseapp.core.parser.Parser
import caseapp.core.util.CaseUtil
import caseapp.util.AnnotationOption
import caseapp.core.util.NameOps.toNameOps
import dataclass.data
import shapeless.Typeable
import caseapp.core.util.Formatter
import caseapp.HelpMessage

/**
 * Provides usage and help messages related to `T`
 */
@data class Help[T](
  args: Seq[Arg],
  appName: String,
  appVersion: String,
  progName: String,
  argsNameOption: Option[String],
  optionsDesc: String = Help.DefaultOptionsDesc,
  nameFormatter: Formatter[Name] = Help.DefaultNameFormatter,
  helpMessage: Option[HelpMessage] = Help.DefaultHelpMessage
) {

  /** One-line usage message for `T` */
  def usage: String =
    Seq(
      "Usage:",
      progName,
      optionsDesc,
      argsNameOption.fold("")("<" + _ + ">")
    ).filter(_.nonEmpty).mkString(" ")

  /** Options description for `T` */
  def options: String = Help.optionsMessage(args, nameFormatter)

  /**
    * Full multi-line help message for `T`.
    *
    * Includes both `usageMessage` and `optionsMessage`
    *
    */
  def help: String = {
    val b = new StringBuilder
    b ++= appName
    if (appVersion.nonEmpty)
      b ++= s" $appVersion"
    b ++= Help.NL
    helpMessage.foreach(msg => b ++= s"${msg.message}${Help.NL}")
    b ++= usage
    b ++= Help.NL
    b ++= options
    b ++= Help.NL
    b.result()
  }

  /**
   * Add help and usage options.
   */
  def withHelp: Help[WithHelp[T]] = {
    final case class Dummy()
    val helpArgs = Parser[WithHelp[Dummy]].args

    withArgs(helpArgs ++ args)
      // circumventing a possible data-class issue here (getting a Help[Nothing] else)
      .asInstanceOf[Help[WithHelp[T]]]
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
}

object Help {
  val DefaultOptionsDesc = "[options]"
  val DefaultNameFormatter = Formatter.DefaultNameFormatter
  val DefaultHelpMessage = Option.empty[HelpMessage]

  /** Look for an implicit `Help[T]` */
  def apply[T](implicit help: Help[T]): Help[T] = help


  /** Option message for `args` */
  def optionsMessage(args: Seq[Arg]): String =
    optionsMessage(args, Formatter.DefaultNameFormatter)

  /** Option message for `args` */
  def optionsMessage(args: Seq[Arg], nameFormatter: Formatter[Name]): String =
    args
      .collect {
        case arg if !arg.noHelp =>

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

  def derive[T]
   (implicit
     parser: Parser[T],
     typeable: Typeable[T],
     appName: AnnotationOption[AppName, T],
     appVersion: AnnotationOption[AppVersion, T],
     progName: AnnotationOption[ProgName, T],
     argsName: AnnotationOption[ArgsName, T],
     helpMessage: AnnotationOption[HelpMessage, T],
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
  implicit def help[T]
   (implicit
     parser: Parser[T],
     typeable: Typeable[T],
     appName: AnnotationOption[AppName, T],
     appVersion: AnnotationOption[AppVersion, T],
     progName: AnnotationOption[ProgName, T],
     argsName: AnnotationOption[ArgsName, T],
     helpMessage: AnnotationOption[HelpMessage, T],
   ): Help[T] = {

    val appName0 = appName().fold(typeable.describe.stripSuffix("Options"))(_.appName)

    Help(
      parser.args,
      appName0,
      appVersion().fold("")(_.appVersion),
      progName().fold(CaseUtil.pascalCaseSplit(appName0.toList).map(_.toLowerCase).mkString("-"))(_.progName),
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

}

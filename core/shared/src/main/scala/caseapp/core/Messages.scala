package caseapp
package core

import caseapp.util.AnnotationOption
import shapeless._
import caseapp.core.util.pascalCaseSplit
import shapeless.labelled.FieldType

/**
 * Provides usage and help messages related to `T`
 */
case class Messages[T](
  args: Seq[Arg],
  appName: String,
  appVersion: String,
  progName: String,
  argsNameOption: Option[String],
  optionsDesc: String = "[options]"
) {
  def usageMessage: String =
    s"Usage: $progName $optionsDesc ${argsNameOption.map("<" + _ + ">").mkString}"

  def optionsMessage: String = Messages.optionsMessage(args)

  def helpMessage = {
    val b = new StringBuilder
    b ++= s"$appName $appVersion${Messages.NL}"
    b ++= usageMessage
    b ++= Messages.NL
    b ++= optionsMessage
    b ++= Messages.NL
    b.result()
  }

  /**
   * Add help and usage options to the messages.
   */
  def withHelp: Messages[WithHelp[T]] = {
    case class Dummy()
    val helpArgs = Parser[WithHelp[Dummy]].args

    copy(args = helpArgs ++ args)
  }
}

object Messages {
  def apply[T](implicit messages: Messages[T]): Messages[T] = messages

  def optionsMessage(args: Seq[Arg]): String =
    args.collect { case arg if !arg.noHelp =>
      val names = Name(arg.name) +: arg.extraNames
      // FIXME Flags that accept no value are not given the right help message here
      val valueDescription = arg.valueDescription.orElse(if (!arg.isFlag) Some(ValueDescription("value")) else None)

      val message = arg.helpMessage.map(Messages.TB + _.message)

      val usage = s"${Messages.WW}${names.map(_.option) mkString " | "}  ${valueDescription.map(_.message).mkString}"

      (usage :: message.toList) mkString Messages.NL
    } .mkString(Messages.NL)


  // FIXME Not sure Typeable is fine on Scala JS, should be replaced by something else
  implicit def messages[T]
   (implicit
     parser: Parser[T],
     typeable: Typeable[T],
     appName: AnnotationOption[AppName, T],
     appVersion: AnnotationOption[AppVersion, T],
     progName: AnnotationOption[ProgName, T],
     argsName: AnnotationOption[ArgsName, T]
   ): Messages[T] = {
    val appName0 = appName().fold(typeable.describe)(_.appName)

    Messages(
      parser.args,
      appName0,
      appVersion().fold("")(_.appVersion),
      progName().fold(pascalCaseSplit(appName0.toList).map(_.toLowerCase).mkString("-"))(_.progName),
      argsName().map(_.argsName)
    )
  }

  // From scopt
  val NL = PlatformUtil.NL
  val WW = "  "
  val TB = "        "

}

case class CommandMessages(
  args: Seq[Arg],
  argsNameOption: Option[String]
) {
  def usageMessage(progName: String, commandName: String): String =
    s"Usage: $progName $commandName ${argsNameOption.map("<" + _ + ">").mkString}"

  def optionsMessage: String = Messages.optionsMessage(args)

  def helpMessage(progName: String, commandName: String): String = {
    val b = new StringBuilder
    b ++= s"Command: $commandName${Messages.NL}"
    b ++= usageMessage(progName, commandName)
    b ++= Messages.NL
    b ++= optionsMessage
    b ++= Messages.NL
    b.result()
  }
}

case class CommandsMessages[T](
  messages: Seq[(String, CommandMessages)]
) {
  lazy val messagesMap = messages.toMap
}

object CommandsMessages {
  def apply[T](implicit messages: CommandsMessages[T]): CommandsMessages[T] = messages

  implicit val cnil: CommandsMessages[CNil] =
    CommandsMessages[CNil](Nil)

  implicit def ccons[K <: Symbol, H, T <: Coproduct]
   (implicit
     key: Witness.Aux[K],
     commandName: AnnotationOption[CommandName, H],
     parser: Lazy[Parser[H]],
     argsName: AnnotationOption[ArgsName, T],
     tail: CommandsMessages[T]
   ): CommandsMessages[FieldType[K, H] :+: T] = {
    // FIXME Duplicated in CommandParser.ccons
    val name = commandName().map(_.commandName).getOrElse {
      pascalCaseSplit(key.value.name.toList.takeWhile(_ != '$'))
        .map(_.toLowerCase)
        .mkString("-")
    }

    CommandsMessages((name -> CommandMessages(
      parser.value.args,
      argsName().map(_.argsName)
    )) +: tail.messages)
  }

  implicit def generic[S, C <: Coproduct]
   (implicit
     gen: LabelledGeneric.Aux[S, C],
     underlying: Lazy[CommandsMessages[C]]
   ): CommandsMessages[S] =
    CommandsMessages(underlying.value.messages)
}

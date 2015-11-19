package caseapp
package core

import derive.AnnotationOption
import shapeless._
import caseapp.core.util.pascalCaseSplit

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
}

object Messages {
  def apply[T](implicit messages: Messages[T]): Messages[T] = messages

  def optionsMessage(args: Seq[Arg]): String =
    args.map { arg =>
      val names = Name(arg.name) +: arg.extraNames
      // FIXME Flags that accept no value are not given the right help message here
      val valueDescription = arg.valueDescription.orElse(if (!arg.isFlag) Some(ValueDescription("value")) else None)

      val message = arg.helpMessage.map(Messages.TB + _.message)

      val usage = s"${Messages.WW}${names.map(_.option) mkString " | "}  ${valueDescription.map(_.message).mkString}"

      (usage :: message.toList) mkString Messages.NL
    } .mkString(Messages.NL)


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
  val NL = System.getProperty("line.separator")
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
  lazy val map = messages.toMap
}

object CommandsMessages {
  def apply[T](implicit messages: CommandsMessages[T]): CommandsMessages[T] = messages

  implicit val cnil: CommandsMessages[CNil] =
    CommandsMessages[CNil](Nil)

  implicit def ccons[H, T <: Coproduct]
   (implicit
     typeable: Typeable[H],
     commandName: AnnotationOption[CommandName, H],
     parser: Strict[Parser[H]],
     argsName: AnnotationOption[ArgsName, T],
     tail: CommandsMessages[T]
   ): CommandsMessages[H :+: T] = {
    // FIXME Duplicated in CommandParser.ccons
    val name = commandName().map(_.commandName).getOrElse {
      // About the takeWhile: should be handled by Typeable?
      pascalCaseSplit(typeable.describe.toList.takeWhile(_ != '$'))
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
     gen: Generic.Aux[S, C],
     underlying: Strict[CommandsMessages[C]]
   ): CommandsMessages[S] =
    CommandsMessages(underlying.value.messages)
}
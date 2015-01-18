package caseapp

import reflect.runtime.universe.TypeTag
import core._

case class WithHelp[T: DescriptionsOf : TypeTag : NamesOf : ArgParser](
                  base: T
, @ExtraName("h") help: Boolean = false
,                usage: Boolean = false
) extends ArgsApp {

  private val messages = Messages[T]()

  private val baseArgsAppOption = base match {
    case a: ArgsApp => Some(a)
    case _ => None
  }

  private var _remainingArgs = Seq.empty[String]

  def setRemainingArgs(remainingArgs: Seq[String]): Unit =
    baseArgsAppOption match {
      case Some(a) =>
        a setRemainingArgs remainingArgs
      case None =>
        _remainingArgs = remainingArgs
    }

  def remainingArgs: Seq[String] =
    baseArgsAppOption match {
      case Some(a) =>
        a.remainingArgs
      case None =>
        _remainingArgs
    }

  def apply(): Unit =
    if (help) {
      Console println messages.helpMessage
      sys exit 0
    } else if (usage) {
      Console println messages.usageMessage
      sys exit 0
    } else
      baseArgsAppOption.foreach(_())
}

object WithHelp {
  implicit def default[T <: ArgsApp : Default : DescriptionsOf : TypeTag : NamesOf : ArgParser]: Default[WithHelp[T]] = Default.from {
    WithHelp(
      base = Default[T].apply()
    , help = false
    , usage = false
    )
  }
}

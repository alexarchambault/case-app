package caseapp.core

import caseapp.Name
import caseapp.core.util.NameOps.toNameOps

/** Base type for errors during arguments parsing */
sealed abstract class Error extends Product with Serializable {
  def message: String
  def append(that: Error): Error
}

object Error {

  sealed abstract class SimpleError(val message: String) extends Error {
    def append(that: Error): Error = that match {
      case simple: SimpleError => Error.SeveralErrors(this, Seq(simple))
      case Error.SeveralErrors(head, tail) =>
        Error.SeveralErrors(this, head +: tail)
    }
  }

  final case class SeveralErrors(head: SimpleError, tail: Seq[SimpleError]) extends Error {
    def message: String = (head +: tail).map(_.message).mkString("\n")
    def append(that: Error): Error = that match {
      case simple: SimpleError => this.copy(tail = tail :+ simple)
      case Error.SeveralErrors(thatHead, thatTail) =>
        this.copy(tail = tail ++ (thatHead +: thatTail))
    }
  }

  // FIXME These could be made more precise (stating which argument failed, at which position, etc.)

  case object ArgumentMissing extends SimpleError("argument missing")

  case class ArgumentAlreadySpecified(name: String, extraNames: Seq[String] = Nil)
    extends SimpleError(s"argument ${(name +: extraNames).mkString(" / ")} already specified")

  case object CannotBeDisabled extends SimpleError("Option cannot be explicitly disabled")

  final case class UnrecognizedFlagValue(value: String) extends SimpleError(s"Unrecognized flag value: $value")

  final case class UnrecognizedArgument(arg: String) extends SimpleError(s"Unrecognized argument: $arg")

  final case class CommandNotFound(command: String) extends SimpleError(s"Command not found: $command")

  final case class RequiredOptionNotSpecified(name: String, extraNames: Seq[String] = Nil)
    extends SimpleError(s"Required option ${(name +: extraNames).mkString(" / ")} not specified")

  final case class MalformedValue(`type`: String, error: String) extends SimpleError(s"Malformed ${`type`}: $error")

  final case class Other(override val message: String) extends SimpleError(message)


  final case class ParsingArgument(name: Name, error: Error)
    extends SimpleError(s"Argument ${name.option}: ${error.message}")

}

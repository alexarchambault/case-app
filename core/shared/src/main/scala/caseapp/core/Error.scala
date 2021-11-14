package caseapp.core

import caseapp.Name
import caseapp.core.util.NameOps.toNameOps
import dataclass.data
import caseapp.core.util.Formatter

/** Base type for errors during arguments parsing */
sealed abstract class Error extends Product with Serializable {
  def message: String
  def append(that: Error): Error
}

object Error {

  sealed abstract class SimpleError(val message: String) extends Error {
    def append(that: Error): Error = that match {
      case simple: SimpleError => Error.SeveralErrors(this, Seq(simple))
      case s: Error.SeveralErrors =>
        Error.SeveralErrors(this, s.head +: s.tail)
    }
  }

  @data class SeveralErrors(head: SimpleError, tail: Seq[SimpleError]) extends Error {
    def message: String = (head +: tail).map(_.message).mkString("\n")
    def append(that: Error): Error = that match {
      case simple: SimpleError => withTail(tail :+ simple)
      case s: Error.SeveralErrors =>
        withTail(tail ++ (s.head +: s.tail))
    }
  }

  // FIXME These could be made more precise (stating which argument failed, at which position, etc.)

  case object ArgumentMissing extends SimpleError("argument missing")

  @data class ArgumentAlreadySpecified(name: String, extraNames: Seq[String] = Nil)
      extends SimpleError(s"argument ${(name +: extraNames).mkString(" / ")} already specified")

  case object CannotBeDisabled extends SimpleError("Option cannot be explicitly disabled")

  @data class UnrecognizedFlagValue(value: String)
      extends SimpleError(s"Unrecognized flag value: $value")

  @data class UnrecognizedArgument(arg: String) extends SimpleError(s"Unrecognized argument: $arg")

  @data class CommandNotFound(command: String) extends SimpleError(s"Command not found: $command")

  @data class RequiredOptionNotSpecified(name: String, extraNames: Seq[String] = Nil)
      extends SimpleError(s"Required option ${(name +: extraNames).mkString(" / ")} not specified")

  @data class MalformedValue(`type`: String, error: String)
      extends SimpleError(s"Malformed ${`type`}: $error")

  @data class Other(override val message: String) extends SimpleError(message)
  @data class ParsingArgument(name: Name, error: Error, nameFormatter: Formatter[Name])
      extends SimpleError(
        s"Argument ${name.option(nameFormatter)}: ${error.message}"
      )

}

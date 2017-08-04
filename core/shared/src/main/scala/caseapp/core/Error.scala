package caseapp.core

/** Base type for errors during arguments parsing */
sealed abstract class Error(val message: String) extends Product with Serializable

object Error {

  // FIXME These could be made more precise (stating which argument failed, at which position, etc.)

  case object ArgumentMissing extends Error("argument missing")

  case class ArgumentAlreadySpecified(name: String, extraNames: Seq[String] = Nil)
    extends Error(s"argument ${(name +: extraNames).mkString(" / ")} already specified")

  case object CannotBeDisabled extends Error("Option cannot be explicitly disabled")

  final case class UnrecognizedFlagValue(value: String) extends Error(s"Unrecognized flag value: $value")

  final case class UnrecognizedValue(value: String) extends Error(s"Unrecognized value: $value")

  final case class UnrecognizedArgument(arg: String) extends Error(s"Unrecognized argument: $arg")

  final case class CommandNotFound(command: String) extends Error(s"Command not found: $command")

  final case class RequiredOptionNotSpecified(name: String, extraNames: Seq[String] = Nil)
    extends Error(s"Required option ${(name +: extraNames).mkString(" / ")} not specified")

  final case class MalformedValue(`type`: String, error: String) extends Error(s"Malformed ${`type`}: $error")

  final case class Other(override val message: String) extends Error(message)

}

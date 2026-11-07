package caseapp.core

import caseapp.Name
import caseapp.core.util.NameOps.toNameOps
import dataclass.data
import caseapp.core.util.Formatter

/** Base type for errors during arguments parsing
  *
  * Either a single error ([[Error.SimpleError]]), or several ones ([[Error.SeveralErrors]]), as
  * parsing goes on after errors, to report as many of them as possible at once.
  */
sealed abstract class Error extends Product with Serializable {

  /** Human-readable error message (one line per error, for [[Error.SeveralErrors]]) */
  def message: String

  /** Combines this error with `that` */
  def append(that: Error): Error
}

object Error {

  /** A single error */
  sealed abstract class SimpleError(val message: String) extends Error {
    def append(that: Error): Error = that match {
      case simple: SimpleError => Error.SeveralErrors(this, Seq(simple))
      case s: Error.SeveralErrors =>
        Error.SeveralErrors(this, s.head +: s.tail)
    }
  }

  /** Several errors, `head` being the first one */
  @data(
    deprecatedSetters = true,
    deprecatedSettersMessage = "Use copy instead",
    deprecatedSettersSince = "2.1.2"
  ) case class SeveralErrors(head: SimpleError, tail: Seq[SimpleError]) extends Error {
    def message: String = (head +: tail).map(_.message).mkString("\n")
    def append(that: Error): Error = that match {
      case simple: SimpleError => this.copy(tail = tail :+ simple)
      case s: Error.SeveralErrors =>
        this.copy(tail = tail ++ (s.head +: s.tail))
    }
  }

  // FIXME These could be made more precise (stating which argument failed, at which position, etc.)

  /** No value was passed to an option expecting one (for example, when it's the last argument) */
  case object ArgumentMissing extends SimpleError("argument missing")

  /** An argument was specified more than once (not returned by case-app itself) */
  @data(
    deprecatedSetters = true,
    deprecatedSettersMessage = "Use copy instead",
    deprecatedSettersSince = "2.1.2"
  ) case class ArgumentAlreadySpecified(name: String, extraNames: Seq[String] = Nil)
      extends SimpleError(s"argument ${(name +: extraNames).mkString(" / ")} already specified")

  // meant to be wrapped in ParsingArgument, which states the argument name
  /** An option accepting a single value was specified more than once
    *
    * Wrapped in [[ParsingArgument]], which states the option name. Use `List[T]`, or
    * [[caseapp.core.argparser.Last]], for options that can be specified several times.
    */
  case object AlreadySpecified extends SimpleError("specified more than once")

  /** A flag of type `Unit`, that can't be disabled, was passed a false value, like `--foo=false`
    */
  case object CannotBeDisabled extends SimpleError("Option cannot be explicitly disabled")

  /** A flag was passed a value other than `true`, `false`, `1`, or `0` */
  @data(
    deprecatedSetters = true,
    deprecatedSettersMessage = "Use copy instead",
    deprecatedSettersSince = "2.1.2"
  ) case class UnrecognizedFlagValue(value: String)
      extends SimpleError(s"Unrecognized flag value: $value")

  /** An unrecognized option was passed */
  @data(
    deprecatedSetters = true,
    deprecatedSettersMessage = "Use copy instead",
    deprecatedSettersSince = "2.1.2"
  ) case class UnrecognizedArgument(arg: String)
      extends SimpleError(s"Unrecognized argument: $arg")

  /** An unrecognized command was passed (not returned by case-app itself) */
  @data(
    deprecatedSetters = true,
    deprecatedSettersMessage = "Use copy instead",
    deprecatedSettersSince = "2.1.2"
  ) case class CommandNotFound(command: String)
      extends SimpleError(s"Command not found: $command")

  /** An option with no default value wasn't specified */
  @data(
    deprecatedSetters = true,
    deprecatedSettersMessage = "Use copy instead",
    deprecatedSettersSince = "2.1.2"
  ) case class RequiredOptionNotSpecified(name: String, extraNames: Seq[String] = Nil)
      extends SimpleError(s"Required option ${(name +: extraNames).mkString(" / ")} not specified")

  /** A value couldn't be parsed, like `foo` for an `Int`
    *
    * @param `type`
    *   description of the expected type, like `integer`
    * @param error
    *   the value that couldn't be parsed
    */
  @data(
    deprecatedSetters = true,
    deprecatedSettersMessage = "Use copy instead",
    deprecatedSettersSince = "2.1.2"
  ) case class MalformedValue(`type`: String, error: String)
      extends SimpleError(s"Malformed ${`type`}: $error")

  /** Any other error, with a custom message
    *
    * Handy to report errors from custom [[caseapp.core.argparser.ArgParser]]s.
    */
  @data(
    deprecatedSetters = true,
    deprecatedSettersMessage = "Use copy instead",
    deprecatedSettersSince = "2.1.2"
  ) case class Other(override val message: String) extends SimpleError(message)

  /** An error that occurred while parsing the value of option `name` */
  @data(
    deprecatedSetters = true,
    deprecatedSettersMessage = "Use copy instead",
    deprecatedSettersSince = "2.1.2"
  ) case class ParsingArgument(name: Name, error: Error, nameFormatter: Formatter[Name])
      extends SimpleError(
        s"Argument ${name.option(nameFormatter)}: ${error.message}"
      )

}

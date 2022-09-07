package caseapp.core

import caseapp.{Group, HelpMessage, Name, Tag, ValueDescription}
import dataclass._

/** Infos about an argument / option an application can accept.
  *
  * @param name:
  *   main name of the argument
  * @param extraNames:
  *   extra names
  * @param valueDescription:
  *   description of its value (optional)
  * @param helpMessage:
  *   help message for this argument (optional)
  * @param noHelp:
  *   if true, this argument should not appear in help messages
  * @param isFlag:
  *   if true, passing an actual value to this argument is optional
  */
@data class Arg(
  name: Name,
  extraNames: Seq[Name] = Nil,
  valueDescription: Option[ValueDescription] = None,
  helpMessage: Option[HelpMessage] = None,
  noHelp: Boolean = false,
  isFlag: Boolean = false,
  @since
  group: Option[Group] = None,
  @since
  origin: Option[String] = None,
  @since
  tags: Seq[Tag] = Nil
) {
  def withDefaultOrigin(defaultOrigin: String): Arg =
    if (origin.isEmpty) withOrigin(Some(defaultOrigin))
    else this
}

object Arg {
  def apply(name: String): Arg =
    Arg(Name(name))
}

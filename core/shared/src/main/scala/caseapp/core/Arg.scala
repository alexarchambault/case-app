package caseapp.core

import caseapp.{HelpMessage, Name, ValueDescription}

/**
  * Infos about an argument / option an application can accept.
  *
  * @param name: main name of the argument
  * @param extraNames: extra names
  * @param valueDescription: description of its value (optional)
  * @param helpMessage: help message for this argument (optional)
  * @param noHelp: if true, this argument should appear in help messages
  * @param isFlag: if true, passing an actual value to this argument is optional
  */
final case class Arg(
  name: String,
  extraNames: Seq[Name] = Nil,
  valueDescription: Option[ValueDescription] = None,
  helpMessage: Option[HelpMessage] = None,
  noHelp: Boolean = false,
  isFlag: Boolean = false
)

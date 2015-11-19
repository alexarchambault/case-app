package caseapp.core

trait ArgsApp {
  def setRemainingArgs(remainingArgs: Seq[String]): Unit
  def remainingArgs: Seq[String]
  def apply(): Unit
}

trait CommandArgsApp extends ArgsApp {
  def setCommand(cmd: Option[Either[String, String]]): Unit
  def command: Option[Either[String, String]]
}

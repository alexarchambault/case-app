package caseapp.core

trait DefaultCommandArgsApp extends DefaultArgsApp with CommandArgsApp {
  private var command0 = Option.empty[Either[String, String]]

  def setCommand(cmd: Option[Either[String, String]]): Unit = {
    command0 = cmd
  }

  def command: Option[Either[String, String]] = command0
}

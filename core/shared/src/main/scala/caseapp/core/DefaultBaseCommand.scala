package caseapp
package core

final case class DefaultBaseCommand() extends Command {
  override def setCommand(cmd: Option[Either[String, String]]): Unit = {
    if (cmd.isEmpty) {
      // FIXME Print available commands too?
      Console.err.println("Error: no command specified")
      sys.exit(255)
    }
    super.setCommand(cmd)
  }
}

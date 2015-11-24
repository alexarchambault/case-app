package caseapp.core

trait DefaultArgsApp extends ArgsApp {
  private var remainingArgs0 = Seq.empty[String]

  def setRemainingArgs(remainingArgs: Seq[String]): Unit =
    remainingArgs0 = remainingArgs

  def remainingArgs: Seq[String] = remainingArgs0
}

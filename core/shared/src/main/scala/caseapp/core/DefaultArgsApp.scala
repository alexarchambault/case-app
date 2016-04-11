package caseapp.core

trait DefaultArgsApp extends ArgsApp {
  private var remainingArgs0 = Seq.empty[String]
  private var extraArgs0 = Seq.empty[String]

  def setRemainingArgs(remainingArgs: Seq[String], extraArgs: Seq[String]): Unit = {
    remainingArgs0 = remainingArgs
    extraArgs0 = extraArgs
  }

  def remainingArgs: Seq[String] = remainingArgs0 ++ extraArgs0
}

package caseapp.core

trait ArgsApp {
  def setRemainingArgs(remainingArgs: Seq[String]): Unit
  def remainingArgs: Seq[String]
  def apply(): Unit
}

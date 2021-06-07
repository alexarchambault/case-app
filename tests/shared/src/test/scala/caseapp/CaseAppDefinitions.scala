package caseapp

import caseapp.core.Error

object CaseAppDefinitions {

  object HasHelp {
    final class Errored(val error: Error) extends Exception(error.message)
    final case class Options()
    object App extends CaseApp[Options] {
      override def hasHelp = false
      override def error(error: Error) =
        throw new Errored(error)
      def run(options: Options, args: RemainingArgs): Unit =
        ???
    }
  }

}

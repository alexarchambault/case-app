package caseapp.core.app

import caseapp.core.help.WithHelp
import caseapp.core.parser.Parser
import caseapp.core.{Error, RemainingArgs}

private[caseapp] sealed trait CaseAppOutput[+T]
private[caseapp] case class Run[+T](t: T, remainingArgs: RemainingArgs) extends CaseAppOutput[T]
private[caseapp] case class ErrorOut(error: Error) extends CaseAppOutput[Nothing]
private[caseapp] case object HelpOut extends CaseAppOutput[Nothing]
private[caseapp] case object UsageOut extends CaseAppOutput[Nothing]

private[caseapp] object AppRunners {

  def interpretCaseArgs[T](
    args: List[String]
  )(implicit parser: Parser[T]): CaseAppOutput[T] =
    parser.withHelp.detailedParse(args) match {
      case Left(err) => ErrorOut(err)
      case Right((WithHelp(true, _, _), _)) => UsageOut
      case Right((WithHelp(_, true, _), _)) => HelpOut
      case Right((WithHelp(_, _, baseOrError), remainingArgs)) =>
        baseOrError.fold(
          ErrorOut,
          Run[T](_, remainingArgs)
        )
    }
}

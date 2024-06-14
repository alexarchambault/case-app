package caseapp.core.help

import caseapp.{AppName, AppVersion, ArgsName, ProgName}
import caseapp.core.parser.Parser
import caseapp.core.util.CaseUtil
import caseapp.util.AnnotationOption
import caseapp.HelpMessage
import shapeless.Typeable

abstract class HelpCompanion {

  // FIXME Not sure Typeable is fine on Scala JS, should be replaced by something else

  def derive[T](implicit
    parser: Parser[T],
    typeable: Typeable[T],
    appName: AnnotationOption[AppName, T],
    appVersion: AnnotationOption[AppVersion, T],
    progName: AnnotationOption[ProgName, T],
    argsName: AnnotationOption[ArgsName, T],
    helpMessage: AnnotationOption[HelpMessage, T]
  ): Help[T] =
    help[T](
      parser,
      typeable,
      appName,
      appVersion,
      progName,
      argsName,
      helpMessage
    )

  /** Implicitly derives a `Help[T]` for `T` */
  implicit def help[T](implicit
    parser: Parser[T],
    typeable: Typeable[T],
    appName: AnnotationOption[AppName, T],
    appVersion: AnnotationOption[AppVersion, T],
    progName: AnnotationOption[ProgName, T],
    argsName: AnnotationOption[ArgsName, T],
    helpMessage: AnnotationOption[HelpMessage, T]
  ): Help[T] = {

    val appName0 = appName() match {
      case None =>
        if (typeable.describe == "Options") typeable.describe
        else typeable.describe.stripSuffix("Options")
      case Some(name) =>
        name.appName
    }

    Help(
      parser.args,
      appName0,
      appVersion().fold("")(_.appVersion),
      progName().fold(CaseUtil.pascalCaseSplit(appName0.toList).map(_.toLowerCase).mkString("-"))(
        _.progName
      ),
      argsName().map(_.argsName),
      Help.DefaultOptionsDesc,
      parser.defaultNameFormatter,
      helpMessage()
    )
  }

}

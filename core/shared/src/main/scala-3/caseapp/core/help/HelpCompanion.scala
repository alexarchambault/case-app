package caseapp.core.help

import caseapp.core.parser.Parser
import caseapp.core.parser.LowPriorityParserImplicits
import caseapp.core.parser.LowPriorityParserImplicits.{annotations, ofOption, summonOrAbort}
import caseapp.core.util.CaseUtil

import scala.quoted.*

object HelpCompanion {
  inline def deriveHelp[T]: Help[T] =
    ${ deriveHelpImpl }
  def deriveHelpImpl[T](using q: Quotes, t: Type[T]): Expr[Help[T]] = {
    import quotes.reflect.*
    val sym        = TypeRepr.of[T].dealias.typeSymbol
    val parserExpr = summonOrAbort[Parser[T]](s"Cannot derive a help message for ${Type.show[T]}")
    val appName = annotations[caseapp.AppName](sym).headOption
      .map(appName => '{ $appName.appName })
      .getOrElse {
        val shortName = LowPriorityParserImplicits.shortName[T]
        Expr(if (shortName == "Options") shortName else shortName.stripSuffix("Options"))
      }
    val appVersion = annotations[caseapp.AppVersion](sym).headOption
      .map(appVersion => '{ $appVersion.appVersion })
      .getOrElse(Expr(""))
    val progName = annotations[caseapp.ProgName](sym).headOption
      .map(progName => '{ $progName.progName })
    val argsName = annotations[caseapp.ArgsName](sym).headOption
      .map(argsName => '{ $argsName.argsName })
    val helpMessage = annotations[caseapp.HelpMessage](sym).headOption
    '{
      val parser   = $parserExpr
      val appName0 = $appName
      val progName0 = ${ Expr.ofOption(progName) }.getOrElse {
        CaseUtil.pascalCaseSplit(appName0.toList).map(_.toLowerCase).mkString("-")
      }
      Help(
        args = parser.args,
        appName = appName0,
        appVersion = $appVersion,
        progName = progName0,
        argsNameOption = ${ Expr.ofOption(argsName) },
        optionsDesc = Help.DefaultOptionsDesc,
        nameFormatter = parser.defaultNameFormatter,
        helpMessage = ${ Expr.ofOption(helpMessage) }
      )
    }
  }
}

abstract class HelpCompanion {
  inline given derive[T]: Help[T] =
    HelpCompanion.deriveHelp[T]

  /** Allows to write `case class Options(...) derives Help` */
  inline def derived[T]: Help[T] =
    HelpCompanion.deriveHelp[T]
}

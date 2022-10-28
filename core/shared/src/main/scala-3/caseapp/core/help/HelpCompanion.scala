package caseapp.core.help

import caseapp.core.parser.Parser
import caseapp.core.parser.LowPriorityParserImplicits
import caseapp.core.parser.LowPriorityParserImplicits.ofOption
import caseapp.core.util.CaseUtil

import scala.compiletime.*
import scala.deriving.*
import scala.quoted.{given, *}

object HelpCompanion {
  inline def deriveHelp[T]: Help[T] =
    ${ deriveHelpImpl }
  def deriveHelpImpl[T](using q: Quotes, t: Type[T]): Expr[Help[T]] = {
    import quotes.reflect.*
    val sym = TypeTree.of[T].symbol
    val parserExpr = Implicits.search(TypeRepr.of[Parser[T]]) match {
      case iss: ImplicitSearchSuccess =>
        iss.tree.asExpr.asExprOf[Parser[T]]
      case isf: ImplicitSearchFailure =>
        throw new Exception(s"No given ${Type.show[Parser[T]]} instance found")
    }
    val appName = sym.annotations
      .find(_.tpe =:= TypeRepr.of[caseapp.AppName])
      .collect {
        case Apply(_, List(arg)) =>
          arg.asExprOf[String]
      }
      .getOrElse {
        Expr(LowPriorityParserImplicits.shortName[T].stripSuffix("Options"))
      }
    val appVersion = sym.annotations
      .find(_.tpe =:= TypeRepr.of[caseapp.AppVersion])
      .collect {
        case Apply(_, List(arg)) =>
          arg.asExprOf[String]
      }
      .getOrElse(Expr(""))
    val progName = sym.annotations
      .find(_.tpe =:= TypeRepr.of[caseapp.ProgName])
      .collect {
        case Apply(_, List(arg)) =>
          arg.asExprOf[String]
      }
    val argsName = sym.annotations
      .find(_.tpe =:= TypeRepr.of[caseapp.ArgsName])
      .collect {
        case Apply(_, List(arg)) =>
          arg.asExprOf[String]
      }
    val helpMessage = sym.annotations
      .find(_.tpe =:= TypeRepr.of[caseapp.HelpMessage])
      .collect {
        case Apply(_, List(arg, argMd)) =>
          '{ caseapp.HelpMessage(${ arg.asExprOf[String] }, ${ argMd.asExprOf[String] }) }
      }
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
}

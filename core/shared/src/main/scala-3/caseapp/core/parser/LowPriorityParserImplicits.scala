package caseapp.core.parser

import caseapp.core.Arg
import caseapp.core.argparser.ArgParser

import scala.compiletime.*
import scala.deriving.*
import scala.quoted.{given, *}

object LowPriorityParserImplicits {

  // adapted from https://github.com/com-lihaoyi/upickle/blob/3f9c98c3983a98c5e0716e42314d5f825bafa9e4/implicits/src-3/upickle/implicits/macros.scala#L51-L70
  inline private def fieldLabels[T] = ${ fieldLabelsImpl[T] }
  private def fieldLabelsImpl[T](using Quotes, Type[T]): Expr[List[String]] =
    import quotes.reflect.*
    val fields = TypeRepr.of[T].typeSymbol
      .primaryConstructor
      .paramSymss
      .flatten
      .filterNot(_.isType)
      .map(_.name)

    Expr.ofList(fields.map(Expr(_)))
  end fieldLabelsImpl

  extension (comp: Expr.type) {
    def ofOption[T](opt: Option[Expr[T]])(using Quotes, Type[T]): Expr[Option[T]] =
      opt match {
        case None        => '{ None }
        case Some(tExpr) => '{ Some($tExpr) }
      }
  }

  def shortName[T](using Quotes, Type[T]): String = {
    val fullName = Type.show[T]
    // attempt at getting a simple name out of fullName (this is likely broken)
    fullName.takeWhile(_ != '[').split('.').last
  }

  inline private def tupleParser[T]: Parser[_] =
    ${ tupleParserImpl[T] }
  private def tupleParserImpl[T](using q: Quotes, t: Type[T]): Expr[Parser[_]] = {
    import quotes.reflect.*
    val tSym   = TypeTree.of[T].symbol
    val origin = shortName[T]
    val fields = tSym.primaryConstructor.paramSymss.flatten.map(f => (f, f.tree)).collect {
      case (sym, v: ValDef) => (sym, v.tpt.tpe)
    }

    val defaultMap: Map[String, Expr[Any]] = {
      val comp =
        if (tSym.isClassDef && !tSym.companionClass.isNoSymbol) tSym.companionClass
        else tSym
      val bodyOpt = Some(comp)
        .filter(!_.isNoSymbol)
        .map(_.tree)
        .collect {
          case cd: ClassDef => cd.body
        }
      bodyOpt match {
        case Some(body) =>
          val names = fields
            .map(_._1)
            .filter(_.flags.is(Flags.HasDefault))
            .map(_.name)
          val values = body.collect {
            case d @ DefDef(name, _, _, _) if name.startsWith("$lessinit$greater$default") =>
              Ref(d.symbol).asExpr
          }
          names.zip(values).toMap
        case None =>
          Map.empty
      }
    }

    val parserExpr = fields
      .foldRight[(TypeRepr, Expr[Parser[_]])]((TypeRepr.of[EmptyTuple], '{ NilParser })) {
        case ((sym, symTpe), (tailType, tailParserExpr)) =>
          val isRecursive = sym.annotations.exists(_.tpe =:= TypeRepr.of[caseapp.Recurse])
          val extraNames = sym.annotations
            .filter(_.tpe =:= TypeRepr.of[caseapp.ExtraName])
            .collect {
              case Apply(_, List(arg)) =>
                '{ caseapp.ExtraName(${ arg.asExprOf[String] }) }
            }
          val valueDescription = sym.annotations
            .find(_.tpe =:= TypeRepr.of[caseapp.ValueDescription])
            .collect {
              case Apply(_, List(arg)) =>
                '{ caseapp.ValueDescription(${ arg.asExprOf[String] }) }
            }
          val helpMessage = sym.annotations
            .find(_.tpe =:= TypeRepr.of[caseapp.HelpMessage])
            .collect {
              case Apply(_, List(arg, argMd)) =>
                '{ caseapp.HelpMessage(${ arg.asExprOf[String] }, ${ argMd.asExprOf[String] }) }
            }
          val hidden = sym.annotations.exists(_.tpe =:= TypeRepr.of[caseapp.Hidden])
          val group = sym.annotations
            .find(_.tpe =:= TypeRepr.of[caseapp.Group])
            .collect {
              case Apply(_, List(arg)) =>
                '{ caseapp.Group(${ arg.asExprOf[String] }) }
            }
          val tags = sym.annotations
            .filter(_.tpe =:= TypeRepr.of[caseapp.annotation.Tag])
            .collect {
              case Apply(_, List(arg)) =>
                '{ caseapp.annotation.Tag(${ arg.asExprOf[String] }) }
            }
          val newTailType = TypeRepr.of[*:].appliedTo(List(symTpe, tailType))
          val expr = symTpe.asType match {
            case '[t] =>
              given Quotes = q
              lazy val headParserExpr = Implicits.search(TypeRepr.of[Parser[t]]) match {
                case iss: ImplicitSearchSuccess =>
                  iss.tree.asExpr
                case isf: ImplicitSearchFailure =>
                  throw new Exception(s"No given ${Type.show[Parser[t]]} instance found")
              }
              lazy val argumentExpr = {
                val default = defaultMap.get(sym.name).map(_.asExprOf[t])
                val argParserExpr = Implicits.search(TypeRepr.of[ArgParser[t]]) match {
                  case iss: ImplicitSearchSuccess =>
                    iss.tree.asExpr.asExprOf[ArgParser[t]]
                  case isf: ImplicitSearchFailure =>
                    throw new Exception(s"No given ${Type.show[ArgParser[t]]} instance found")
                }
                '{
                  val argParser = $argParserExpr
                  val valueDesc = ${ Expr.ofOption(valueDescription) }
                    .getOrElse(caseapp.ValueDescription(argParser.description))
                  val arg = Arg(
                    name = caseapp.Name(${ Expr(sym.name) }),
                    extraNames = ${ Expr.ofList(extraNames) },
                    valueDescription = Some(valueDesc),
                    helpMessage = ${ Expr.ofOption(helpMessage) },
                    noHelp = ${ Expr(hidden) },
                    isFlag = argParser.isFlag,
                    group = ${ Expr.ofOption(group) },
                    origin = None,
                    // sorting, as the ordering provided by Scala 3 doesn't seem to always respect the one in the original sources
                    tags = ${ Expr.ofList(tags) }.sortBy(_.name)
                  )
                  Argument[t](arg, argParser, () => ${ Expr.ofOption(default) })
                }
              }
              tailType.asType match {
                case '[EmptyTuple] =>
                  if (isRecursive)
                    '{
                      RecursiveConsParser[t, EmptyTuple](
                        ${ headParserExpr.asExprOf[Parser[t]] },
                        ${ tailParserExpr.asExprOf[Parser[EmptyTuple]] }
                      )
                    }
                  else
                    '{
                      ConsParser[t, EmptyTuple](
                        $argumentExpr,
                        ${ tailParserExpr.asExprOf[Parser[EmptyTuple]] }
                      )
                    }

                case '[head *: tail] =>
                  if (isRecursive)
                    '{
                      RecursiveConsParser[t, head *: tail](
                        ${ headParserExpr.asExprOf[Parser[t]] },
                        ${ tailParserExpr.asExprOf[Parser[head *: tail]] }
                      )
                    }
                  else
                    '{
                      ConsParser[t, head *: tail](
                        $argumentExpr,
                        ${ tailParserExpr.asExprOf[Parser[head *: tail]] }
                      )
                    }
              }
          }
          (newTailType, expr)
      }
      ._2
      .asExprOf[Parser[_]]

    '{ $parserExpr.withDefaultOrigin(${ Expr(origin) }) }
  }
}

trait LowPriorityParserImplicits {
  inline given derive[T](using m: Mirror.ProductOf[T]): Parser[T] =
    LowPriorityParserImplicits.tupleParser[T].asInstanceOf[Parser[m.MirroredElemTypes]].map(
      m.fromTuple
    )
}

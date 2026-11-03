package caseapp.core.parser

import caseapp.core.Arg
import caseapp.core.argparser.ArgParser
import caseapp.core.default.Default

import scala.deriving.*
import scala.quoted.*

object LowPriorityParserImplicits {

  extension (comp: Expr.type) {
    def ofOption[T](opt: Option[Expr[T]])(using Quotes, Type[T]): Expr[Option[T]] =
      opt match {
        case None        => '{ None }
        case Some(tExpr) => '{ Some($tExpr) }
      }
  }

  def shortName[T](using Quotes, Type[T]): String = {
    import quotes.reflect.*
    TypeRepr.of[T].dealias.typeSymbol.name.stripSuffix("$")
  }

  /** The annotations of type `A` of a symbol, in source order */
  private[core] def annotations[A: Type](using q: Quotes)(sym: q.reflect.Symbol): List[Expr[A]] = {
    import q.reflect.*
    // Symbol#annotations starts from the last annotation of the sources
    sym.annotations.reverse.filter(_.tpe =:= TypeRepr.of[A]).map(_.asExprOf[A])
  }

  /** Looks for an implicit `A`, failing the compilation with `message` and the reason why if there
    * isn't any
    */
  private[core] def summonOrAbort[A: Type](message: String)(using Quotes): Expr[A] = {
    import quotes.reflect.*
    Implicits.search(TypeRepr.of[A]) match {
      case success: ImplicitSearchSuccess => success.tree.asExprOf[A]
      case failure: ImplicitSearchFailure =>
        report.errorAndAbort(s"$message: ${failure.explanation}")
    }
  }

  private def fields[U](using
    q: Quotes,
    t: Type[U]
  ): List[(q.reflect.Symbol, q.reflect.TypeRepr)] = {
    import quotes.reflect.*
    val tpe = TypeRepr.of[U].dealias
    val sym = tpe.typeSymbol
    sym.primaryConstructor.paramSymss.flatten
      .filter(_.isTerm)
      .map(param => (param, tpe.memberType(sym.fieldMember(param.name))))
  }

  /** The default values of the fields of `U`, by field name */
  private def defaultValues[U](using q: Quotes, t: Type[U]): Map[String, q.reflect.Term] = {
    import quotes.reflect.*
    val (tycon, typeArgs) = TypeRepr.of[U].dealias match {
      case AppliedType(base, args) => (base, args)
      case other                   => (other, Nil)
    }
    val companionSym                          = tycon.typeSymbol.companionModule
    def isInCompanion(owner: Symbol): Boolean =
      !owner.isNoSymbol && (owner == companionSym.moduleClass || isInCompanion(owner.maybeOwner))
    val companion = tycon match {
      // Happens with derives clauses. Ref.term can't refer to an enclosing object.
      case _ if isInCompanion(Symbol.spliceOwner) => This(companionSym.moduleClass)
      case TypeRef(NoPrefix(), _)                 => Ref(companionSym)
      // Going through the prefix, as the companion of a class nested in a class is not static
      case TypeRef(prefix, _) => Ref.term(TermRef(prefix, companionSym.name))
      case _                  => Ref(companionSym)
    }
    fields[U].map(_._1).zipWithIndex.collect {
      case (field, idx) if field.flags.is(Flags.HasDefault) =>
        val getter = companionSym.methodMember(s"$$lessinit$$greater$$default$$${idx + 1}").head
        val value  = companion.select(getter)
        field.name -> (if (typeArgs.isEmpty) value else value.appliedToTypes(typeArgs))
    }.toMap
  }

  inline private def tupleParser[T]: Parser[Tuple] =
    ${ tupleParserImpl[T] }
  private def tupleParserImpl[T](using Quotes, Type[T]): Expr[Parser[Tuple]] = {
    import quotes.reflect.*

    val defaults = defaultValues[T]

    // Each field adds itself to the parser of the fields that follow it. Building the whole parser
    // as a single nested expression would make the compiler overflow its stack on large classes.
    val addFields = fields[T].map {
      case (sym, symTpe) =>
        symTpe.asType match {
          case '[t] =>
            annotations[caseapp.Recurse](sym).headOption match {
              case Some(recurse) =>
                val headParser = summonOrAbort[Parser[t]](
                  s"Cannot derive a parser for field ${sym.name} of ${Type.show[T]}"
                )
                '{ (tail: Parser[Tuple]) =>
                  RecursiveConsParser[t, Tuple]($headParser, tail, $recurse): Parser[Tuple]
                }
              case None =>
                val argParser = summonOrAbort[ArgParser[t]](
                  s"Cannot parse field ${sym.name} of ${Type.show[T]}"
                )
                val default = defaults.get(sym.name).map(_.asExprOf[t]).orElse {
                  Implicits.search(TypeRepr.of[Default[t]]) match {
                    case success: ImplicitSearchSuccess =>
                      Some('{ ${ success.tree.asExprOf[Default[t]] }.value })
                    case _: ImplicitSearchFailure => None
                  }
                }
                val valueDescription = annotations[caseapp.ValueDescription](sym).headOption
                val extraNames       = annotations[caseapp.ExtraName](sym)
                val helpMessage      = annotations[caseapp.HelpMessage](sym).headOption
                val hidden           = annotations[caseapp.Hidden](sym).nonEmpty
                val group            = annotations[caseapp.Group](sym).headOption
                val tags             = annotations[caseapp.annotation.Tag](sym)
                val argumentExpr     = '{
                  val argParser0 = $argParser
                  val valueDesc  = ${ Expr.ofOption(valueDescription) }
                    .getOrElse(caseapp.ValueDescription(argParser0.description))
                  val arg = Arg(
                    name = caseapp.Name(${ Expr(sym.name) }),
                    extraNames = ${ Expr.ofList(extraNames) },
                    valueDescription = Some(valueDesc),
                    helpMessage = ${ Expr.ofOption(helpMessage) },
                    noHelp = ${ Expr(hidden) },
                    isFlag = argParser0.isFlag,
                    group = ${ Expr.ofOption(group) },
                    origin = None,
                    tags = ${ Expr.ofList(tags) }
                  )
                  Argument[t](arg, argParser0, () => ${ Expr.ofOption(default) })
                }
                '{ (tail: Parser[Tuple]) =>
                  ConsParser[t, Tuple]($argumentExpr, tail): Parser[Tuple]
                }
            }
        }
    }

    '{
      ${ Expr.ofList(addFields) }
        .foldRight[Parser[Tuple]](NilParser)((addField, tail) => addField(tail))
        .withDefaultOrigin(${ Expr(shortName[T]) })
    }
  }
}

trait LowPriorityParserImplicits {
  inline given derive[T](using m: Mirror.ProductOf[T]): Parser[T] =
    LowPriorityParserImplicits.tupleParser[T].map(m.fromProduct)

  /** Allows to write `case class Options(...) derives Parser` */
  inline def derived[T](using m: Mirror.ProductOf[T]): Parser[T] =
    derive[T]
}

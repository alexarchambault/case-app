package caseapp.core.parser

import caseapp.core.Arg
import caseapp.core.argparser.ArgParser

import scala.compiletime.*
import scala.deriving.*
import scala.quoted.{given, *}

object LowPriorityParserImplicits {

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

  private def fields[U](using
    q: Quotes,
    t: Type[U]
  ): List[(q.reflect.Symbol, q.reflect.TypeRepr)] = {
    import quotes.reflect.*
    val tpe = TypeRepr.of[U]
    val sym = TypeRepr.of[U] match {
      case AppliedType(base, params) =>
        base.typeSymbol
      case _ =>
        TypeTree.of[U].symbol
    }

    // Many things inspired by https://github.com/plokhotnyuk/jsoniter-scala/blob/8f39e1d45fde2a04984498f036cad93286344c30/jsoniter-scala-macros/shared/src/main/scala-3/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMaker.scala#L564-L613
    // and around, here

    def typeArgs(tpe: TypeRepr): List[TypeRepr] = tpe match
      case AppliedType(_, typeArgs) => typeArgs.map(_.dealias)
      case _                        => Nil

    def resolveParentTypeArg(
      child: Symbol,
      fromNudeChildTarg: TypeRepr,
      parentTarg: TypeRepr,
      binding: Map[String, TypeRepr]
    ): Map[String, TypeRepr] =
      if (fromNudeChildTarg.typeSymbol.isTypeParam) { // todo: check for paramRef instead ?
        val paramName = fromNudeChildTarg.typeSymbol.name
        binding.get(paramName) match
          case None => binding.updated(paramName, parentTarg)
          case Some(oldBinding) =>
            if (oldBinding =:= parentTarg) binding
            else sys.error(
              s"Type parameter $paramName in class ${child.name} appeared in the constructor of " +
                s"${tpe.show} two times differently, with ${oldBinding.show} and ${parentTarg.show}"
            )
      }
      else if (fromNudeChildTarg <:< parentTarg)
        binding // TODO: assupe parentTag is covariant, get covariance from tycon type parameters.
      else
        (fromNudeChildTarg, parentTarg) match
          case (AppliedType(ctycon, ctargs), AppliedType(ptycon, ptargs)) =>
            ctargs.zip(ptargs).foldLeft(resolveParentTypeArg(child, ctycon, ptycon, binding)) {
              (b, e) =>
                resolveParentTypeArg(child, e._1, e._2, b)
            }
          case _ =>
            sys.error(s"Failed unification of type parameters of ${tpe.show} from child $child - " +
              s"${fromNudeChildTarg.show} and ${parentTarg.show}")

    def resolveParentTypeArgs(
      child: Symbol,
      nudeChildParentTags: List[TypeRepr],
      parentTags: List[TypeRepr],
      binding: Map[String, TypeRepr]
    ): Map[String, TypeRepr] =
      nudeChildParentTags.zip(parentTags).foldLeft(binding)((s, e) =>
        resolveParentTypeArg(child, e._1, e._2, s)
      )

    val nudeSubtype      = TypeIdent(sym).tpe
    val baseConst        = nudeSubtype.memberType(sym.primaryConstructor)
    val tpeArgsFromChild = typeArgs(tpe)
    val const = baseConst match {
      case MethodType(_, _, resTp) => resTp
      case PolyType(names, _, resPolyTp) =>
        val targs     = typeArgs(tpe)
        val tpBinding = resolveParentTypeArgs(sym, tpeArgsFromChild, targs, Map.empty)
        val ctArgs = names.map { name =>
          tpBinding.get(name).getOrElse(sys.error(
            s"Type parameter $name of $sym can't be deduced from " +
              s"type arguments of ${tpe.show}. Please provide a custom implicitly accessible codec for it."
          ))
        }
        val polyRes = resPolyTp match
          case MethodType(_, _, resTp) => resTp
          case other                   => other // hope we have no multiple typed param lists yet.
        if (ctArgs.isEmpty) polyRes
        else polyRes match
          case AppliedType(base, _) => base.appliedTo(ctArgs)
          case AnnotatedType(AppliedType(base, _), annot) =>
            AnnotatedType(base.appliedTo(ctArgs), annot)
          case _ => polyRes.appliedTo(ctArgs)
      case other =>
        sys.error(s"Primary constructior for ${tpe.show} is not MethodType or PolyType but $other")
    }
    sym.primaryConstructor
      .paramSymss
      .flatten
      .map(f => (f, f.tree))
      .collect {
        case (sym, v: ValDef) =>
          (sym, v.tpt.tpe)
      }
  }

  inline private def checkFieldCount[T, N <: Int]: Unit =
    ${ checkFieldCountImpl[T, N] }
  private def checkFieldCountImpl[T, N <: Int](using Quotes, Type[T], Type[N]): Expr[Unit] = {
    import quotes.reflect.*

    val viaMirror = TypeRepr.of[N] match {
      case ConstantType(c) =>
        c.value match {
          case n: Int => n
          case other => sys.error(
              s"Expected literal integer type, got ${Type.show[N]} ($other, ${other.getClass})"
            )
        }
      case other =>
        sys.error(s"Expected literal integer type, got ${Type.show[N]} ($other, ${other.getClass})")
    }

    val viaReflect = fields[T].length

    assert(
      viaMirror == viaReflect,
      s"Got Unexpected number of field via reflection for type ${Type.show[T]} (got $viaReflect, expected $viaMirror)"
    )

    '{ () }
  }

  inline private def tupleParser[T]: Parser[_] =
    ${ tupleParserImpl[T] }
  private def tupleParserImpl[T](using q: Quotes, t: Type[T]): Expr[Parser[_]] = {
    import quotes.reflect.*
    val tSym    = TypeTree.of[T].symbol
    val origin  = shortName[T]
    val fields0 = fields[T]

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
          val names = fields0
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

    val parserExpr = fields0
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
  inline given derive[T](using m: Mirror.ProductOf[T]): Parser[T] = {
    LowPriorityParserImplicits.checkFieldCount[T, Tuple.Size[m.MirroredElemTypes]]
    val parser = LowPriorityParserImplicits.tupleParser[T]
    parser.asInstanceOf[Parser[m.MirroredElemTypes]].map(m.fromTuple)
  }
}

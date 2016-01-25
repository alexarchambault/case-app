package caseapp.util

import shapeless._

import scala.reflect.macros.Context

class AnnotationListMacros[C <: Context](val c: C) extends CaseClassMacros {
  import c.universe._

  def consTpe = typeOf[scala.::[_]].typeConstructor
  def nilTpe = typeOf[Nil.type]

  // FIXME Most of the content of this method is cut-n-pasted from generic.scala
  def construct(tpe: Type): List[Tree] => Tree = {
    // FIXME Cut-n-pasted from generic.scala
    val sym = tpe.typeSymbol
    val isCaseClass = sym.asClass.isCaseClass
    def hasNonGenericCompanionMember(name: String): Boolean = {
      val mSym = sym.companionSymbol.typeSignature.member(newTermName(name))
      mSym != NoSymbol && !isNonGeneric(mSym)
    }

    if(isCaseClass || hasNonGenericCompanionMember("apply"))
      args => q"${companionRef(tpe)}(..$args)"
    else
      args => q"new $tpe(..$args)"
  }

  def materializeAnnotationList[A: WeakTypeTag, T: WeakTypeTag, Out: WeakTypeTag]: Tree = {
    val annTpe = weakTypeOf[A]

    if (!isProduct(annTpe))
      abort(s"$annTpe is not a case class-like type")

    val construct0 = construct(annTpe)

    val tpe = weakTypeOf[T]

    val annTreeLists =
      if (isProduct(tpe)) {
        val constructorSyms = tpe
          .member(nme.CONSTRUCTOR)
          .asMethod
          .paramss
          .flatten
          .map { sym => sym.name.decodedName.toString -> sym }
          .toMap

        val fields = fieldsOf(tpe)
        // Looking at these unveils extra annotations below
        // (what we call a "side effect")
        fields.map { case (name, _) =>
          tpe.member(name).annotations
        }

        fields.map { case (name, _) =>
          val paramConstrSym = constructorSyms(name.decodedName.toString)

          paramConstrSym.annotations.collect {
            case ann if ann.tpe =:= annTpe => construct0(ann.scalaArgs)
          }
        }
      } else if (isCoproduct(tpe))
        ctorsOf(tpe).map { cTpe =>
          cTpe.typeSymbol.annotations.collect {
            case ann if ann.tpe =:= annTpe => construct0(ann.scalaArgs)
          }
        }
      else
        abort(s"$tpe is not case class like or the root of a sealed family of types")

    val wrapTpeTrees = annTreeLists.map {
      case Nil => nilTpe -> q"_root_.scala.Nil"
      case l =>
        def listTree(trees: List[Tree]): Tree = {
          import scala.::
          trees match {
            case Nil => q"_root_.scala.Nil"
            case h :: t => q"_root_.scala.::($h, ${listTree(t)})"
          }
        }

        appliedType(consTpe, List(annTpe)) -> listTree(l)
    }

    val outTpe = mkHListTpe(wrapTpeTrees.map { case (aTpe, _) => aTpe })
    val outTree = wrapTpeTrees.foldRight(q"_root_.shapeless.HNil": Tree) {
      case ((_, bound), acc) => pq"_root_.shapeless.::($bound, $acc)"
    }

    q"_root_.caseapp.util.AnnotationList.instance[$annTpe, $tpe, $outTpe]($outTree)"
  }
}

object AnnotationListMacros {
  def inst(c: Context) = new AnnotationListMacros[c.type](c)

  def materializeAnnotationList[A: c.WeakTypeTag, T: c.WeakTypeTag, Out <: HList : c.WeakTypeTag](c: Context): c.Expr[AnnotationList.Aux[A, T, Out]] =
    c.Expr[AnnotationList.Aux[A, T, Out]](inst(c).materializeAnnotationList[A, T, Out])
}
package caseapp.util

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/** Like [[shapeless.LowPriority]], but fine with the "aux pattern" (by "stripping refinements", internally) */
sealed abstract class LowPriority extends Serializable

object LowPriority {

  implicit def materialize: LowPriority = macro LowPriorityMacros.mkLowPriority

}

class LowPriorityMacros(val c: whitebox.Context) extends shapeless.OpenImplicitMacros with shapeless.LowPriorityTypes {
  import c.universe._

  def strictTpe = typeOf[shapeless.Strict[_]].typeConstructor

  def stripRefinements(tpe: Type): Option[Type] =
    tpe match {
      case RefinedType(parents, _) => Some(parents.head)
      case _ => None
    }

  def mkLowPriority: Tree =
    secondOpenImplicitTpe match {
      case Some(tpe) =>
        c.inferImplicitValue(
          appliedType(strictTpe, appliedType(lowPriorityForTpe, stripRefinements(tpe.dealias).getOrElse(tpe))),
          silent = false
        )

        q"null: _root_.caseapp.util.LowPriority"

      case None =>
        c.abort(c.enclosingPosition, "Can't get looked for implicit type")
    }

}

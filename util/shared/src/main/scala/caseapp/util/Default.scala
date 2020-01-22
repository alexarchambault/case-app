package caseapp.util

import shapeless.{::, CaseClassMacros, DepFn0, DepFn1, Generic, HList, HNil}

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/*
 * Temporary vendoring shapeless.Default to tweak it (ensure creating instances of it don't create the default values -
 * only calling apply should)
 */
trait Default[T] extends DepFn0 with Serializable {
  type Out <: HList
}

object Default {
  def apply[T](implicit default: Default[T]): Aux[T, default.Out] = default

  // only change from the (current) shapeless impl in the whole file - make the argument by-name
  def mkDefault[T, Out0 <: HList](defaults: => Out0): Aux[T, Out0] =
    new Default[T] {
      type Out = Out0
      def apply() = defaults
    }

  type Aux[T, Out0 <: HList] = Default[T] { type Out = Out0 }

  implicit def materialize[T, L <: HList]: Aux[T, L] = macro DefaultMacros.materialize[T, L]


  trait AsOptions[T] extends DepFn0 with Serializable {
    type Out <: HList
  }

  object AsOptions {
    def apply[T](implicit default: AsOptions[T]): Aux[T, default.Out] = default

    type Aux[T, Out0 <: HList] = AsOptions[T] { type Out = Out0 }

    trait Helper[L <: HList, Repr <: HList] extends DepFn1[L] with Serializable {
      type Out <: HList
    }

    object Helper {
      def apply[L <: HList, Repr <: HList](implicit helper: Helper[L, Repr]): Aux[L, Repr, helper.Out] = helper

      type Aux[L <: HList, Repr <: HList, Out0 <: HList] = Helper[L, Repr] { type Out = Out0 }

      implicit def hnilHelper: Aux[HNil, HNil, HNil] =
        new Helper[HNil, HNil] {
          type Out = HNil
          def apply(l: HNil) = HNil
        }

      implicit def hconsSomeHelper[H, T <: HList, ReprT <: HList, OutT <: HList]
       (implicit
         tailHelper: Aux[T, ReprT, OutT]
       ): Aux[Some[H] :: T, H :: ReprT, Option[H] :: OutT] =
        new Helper[Some[H] :: T, H :: ReprT] {
          type Out = Option[H] :: OutT
          def apply(l: Some[H] :: T) = l.head :: tailHelper(l.tail)
        }

      implicit def hconsNoneHelper[H, T <: HList, ReprT <: HList, OutT <: HList]
       (implicit
         tailHelper: Aux[T, ReprT, OutT]
       ): Aux[None.type :: T, H :: ReprT, Option[H] :: OutT] =
        new Helper[None.type :: T, H :: ReprT] {
          type Out = Option[H] :: OutT
          def apply(l: None.type :: T) = None :: tailHelper(l.tail)
        }
    }

    implicit def asOption[T, Repr <: HList, Options <: HList, Out0 <: HList]
     (implicit
       default: Default.Aux[T, Options],
       gen: Generic.Aux[T, Repr],
       helper: Helper.Aux[Options, Repr, Out0]
     ): Aux[T, Out0] =
      new AsOptions[T] {
        type Out = Out0
        def apply() = helper(default())
      }
  }
}

class DefaultMacros(val c: whitebox.Context) extends CaseClassMacros {
  import c.universe._

  def someTpe = typeOf[Some[_]].typeConstructor
  def noneTpe = typeOf[None.type]

  def materialize[T: WeakTypeTag, L: WeakTypeTag]: Tree = {
    val tpe = weakTypeOf[T]
    val cls = classSym(tpe)

    lazy val companion = companionRef(tpe)
    def altCompanion = companion.symbol.info

    val none = q"_root_.scala.None"
    def some(value: Tree) = q"_root_.scala.Some($value)"

    // Symbol.alternatives is missing in Scala 2.10
    def overloadsOf(sym: Symbol) =
      if (sym.isTerm) sym.asTerm.alternatives
      else if (sym.isType) sym :: Nil
      else Nil

    def hasDefaultParams(method: MethodSymbol) =
      method.paramLists.flatten.exists(_.asTerm.isParamWithDefault)

    // The existence of multiple apply overloads with default values gets checked
    // after the macro runs. Their existence can make the macro expansion fail,
    // as multiple overloads can define the functions we look for below, possibly
    // with wrong types, making the compilation fail with the wrong error.
    // We do this check here to detect that beforehand.
    def overloadsWithDefaultParamsIn(tpe: Type) =
      overloadsOf(tpe.member(TermName("apply"))).count {
        alt => alt.isMethod && hasDefaultParams(alt.asMethod)
      }

    def defaultsFor(fields: List[(TermName, Type)]) = for {
      ((_, argTpe), i) <- fields.zipWithIndex
      default = tpe.companion.member(TermName(s"apply$$default$$${i + 1}")) orElse
        altCompanion.member(TermName(s"$$lessinit$$greater$$default$$${i + 1}"))
    } yield if (default.isTerm) {
      val defaultTpe = appliedType(someTpe, devarargify(argTpe))
      val defaultVal = some(q"$companion.$default")
      (defaultTpe, defaultVal)
    } else (noneTpe, none)

    def mkDefault(defaults: List[(Type, Tree)]) = {
      val (types, values) = defaults.unzip
      val outTpe = mkHListTpe(types)
      val outVal = mkHListValue(values)
      q"_root_.caseapp.util.Default.mkDefault[$tpe, $outTpe]($outVal)"
    }

    if (isCaseObjectLike(cls)) return mkDefault(Nil)
    if (!isCaseClassLike(cls)) abort(s"$tpe is not a case class or case class like")

    // ClassSymbol.primaryConstructor is missing in Scala 2.10
    val primaryCtor = overloadsOf(tpe.decl(termNames.CONSTRUCTOR)).find {
      alt => alt.isMethod && alt.asMethod.isPrimaryConstructor
    }.getOrElse {
      c.abort(c.enclosingPosition, s"Cannot get primary constructor of $tpe")
    }.asMethod

    // Checking if the primary constructor has default parameters, and returning
    // a Default instance with non-empty types / values only if that holds.
    // The apply$default$... methods below may still exist without these, if an additional
    // apply method has default parameters. We want to ignore them in this case.
    val hasUniqueDefaults = hasDefaultParams(primaryCtor) && {
      val k = overloadsWithDefaultParamsIn(tpe.companion)
      k == 1 || (k == 0 && overloadsWithDefaultParamsIn(altCompanion) == 1)
    }

    mkDefault {
      val fields = fieldsOf(tpe)
      if (hasUniqueDefaults) defaultsFor(fields)
      else List.fill(fields.size)(noneTpe, none)
    }
  }
}

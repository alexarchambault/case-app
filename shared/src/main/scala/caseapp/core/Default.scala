package caseapp
package core

import shapeless._

/** Type class providing a default value for type `CC` */
trait Default[CC] {
  def apply(): CC
}

object Default extends PlatformDefaults {
  def apply[CC](implicit default: Default[CC]): Default[CC] = default
  
  def instance[CC](default: => CC): Default[CC] = new Default[CC] {
    def apply() = default
  }

  implicit def generic[CC, L <: HList, D <: HList]
   (implicit
     gen: Generic.Aux[CC, L],
     default: Strict[shapeless.Default.Aux[CC, D]],
     defaultOr: Strict[DefaultOr[L, D]]
   ): Default[CC] =
    Default.instance(gen.from(defaultOr.value(default.value())))

  implicit val unit: Default[Unit] = Default.instance(())
  implicit val int: Default[Int] = Default.instance(0)
  implicit val long: Default[Long] = Default.instance(0L)
  implicit val float: Default[Float] = Default.instance(0f)
  implicit val double: Default[Double] = Default.instance(0d)
  implicit val bigDecimal: Default[BigDecimal] = Default.instance(BigDecimal(0))
  implicit val boolean: Default[Boolean] = Default.instance(false)
  implicit val counter: Default[Int @@ Counter] = Default.instance(Tag of 0)
  implicit val string: Default[String] = Default.instance("")

  implicit def option[T]: Default[Option[T]] = Default.instance(None)
  implicit def list[T]: Default[List[T]] = Default.instance(Nil)

}

trait DefaultOr[L <: HList, D <: HList] {
  def apply(d: D): L
}

trait LowPriorityDefaultOr {
  implicit def hconsNone[H, T <: HList, TD <: HList]
   (implicit
     default: Strict[Default[H]],
     tail: DefaultOr[T, TD]
   ): DefaultOr[H :: T, None.type :: TD] =
    DefaultOr.instance { case None :: td =>
      default.value() :: tail(td)
    }
}

object DefaultOr extends LowPriorityDefaultOr {
  def apply[L <: HList, D <: HList](implicit defaultOr: DefaultOr[L, D]): DefaultOr[L, D] =
    defaultOr

  def instance[L <: HList, D <: HList](f: D => L): DefaultOr[L, D] =
    new DefaultOr[L, D] {
      def apply(d: D) = f(d)
    }

  implicit val hnil: DefaultOr[HNil, HNil] =
    instance(_ => HNil)

  implicit def hconsSome[H, T <: HList, TD <: HList]
   (implicit
     tail: DefaultOr[T, TD]
   ): DefaultOr[H :: T, Some[H] :: TD] =
    instance { case Some(d) :: td =>
      d :: tail(td)
    }
}
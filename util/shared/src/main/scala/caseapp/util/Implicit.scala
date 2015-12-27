package caseapp.util

import shapeless._

trait Implicit[T] {
  def value: T
}

trait LowPriorityImplicit {
  implicit def cconsNotFound[H, T <: Coproduct]
   (implicit
     tail: Implicit[T]
   ): Implicit[H :+: T] =
    Implicit.instance(Inr(tail.value))
}

object Implicit extends LowPriorityImplicit {
  def apply[T](implicit impl: Implicit[T]): Implicit[T] = impl

  def instance[T](t: => T): Implicit[T] =
    new Implicit[T] {
      def value = t
    }

  implicit val hnil: Implicit[HNil] =
    instance(HNil)
  implicit def hcons[H, T <: HList]
   (implicit
     head: Implicit[H],
     tail: Implicit[T]
   ): Implicit[H :: T] =
    instance(head.value :: tail.value)

  implicit def ccons[H, T <: Coproduct]
   (implicit
     head: Implicit[H]
   ): Implicit[H :+: T] =
    instance(Inl(head.value))

  implicit def generic[F, G]
   (implicit
     gen: Generic.Aux[F, G],
     impl: Implicit[G]
   ): Implicit[F] =
    instance(gen.from(impl.value))

  implicit def self[T](implicit t: T): Implicit[T] =
    instance(t)
}

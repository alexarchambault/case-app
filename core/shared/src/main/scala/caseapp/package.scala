import shapeless.{ CNil, :+:, Inl, Inr, Generic }

package object caseapp {

  type ExtraName = Name

  type Parser[T] = core.Parser[T]
  val Parser = core.Parser

  type CommandParser[T] = core.CommandParser[T]
  val CommandParser = core.CommandParser

  // Running into weird errors with this one when using Tag.of, so let's use newtype below instead
  // type @@[+T, Tag] = shapeless.tag.@@[T, Tag]
  // object Tag {
  //   def of[Tag] = shapeless.tag[Tag]
  //   def unwrap[T, Tag](t: T @@ Tag): T = t.asInstanceOf[T]
  // }

  // Custom tag implementation, see above for more details
  type @@[T, Tag] = shapeless.newtype.Newtype[T, Tag]
  object Tag {
    case class TagBuilder[Tag]() {
      def apply[T](t: T): T @@ Tag = t.asInstanceOf[T @@ Tag]
    }

    def of[Tag] = TagBuilder[Tag]()
    def unwrap[T, Tag](t: T @@ Tag): T = t.asInstanceOf[T]
  }

  sealed trait Counter

  // required for the implicits involved in default values to be fine
  implicit def optionGeneric[T]: Generic.Aux[Option[T], Some[T] :+: None.type :+: CNil] =
    new Generic[Option[T]] {
      type Repr = Some[T] :+: None.type :+: CNil
      def from(r: Repr) = r.unify
      def to(opt: Option[T]) = opt match {
        case None => Inr(Inl(None))
        case s @ Some(_) => Inl(s)
      }
    }

}

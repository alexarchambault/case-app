package object caseapp {

  type ExtraName = Name

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

}

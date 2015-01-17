package object caseapp {

  type ExtraName = Name

  // Running into weird errors with this one when using Tag.of, so let's use newtype below instead
  // type @@[+T, Tag] = shapeless.tag.@@[T, Tag]
  // val Tag = new {
  //   def of[Tag] = shapeless.tag[Tag]
  //   def unwrap[T, Tag](t: T @@ Tag): T = t.asInstanceOf[T]
  // }

  type @@[T, Tag] = shapeless.newtype.Newtype[T, Tag]
  case class TagBuilder[Tag]() {
    def apply[T](t: T): T @@ Tag = t.asInstanceOf[T @@ Tag]
  }
  val Tag = new {
    def of[Tag] = TagBuilder[Tag]()
    def unwrap[T, Tag](t: T @@ Tag): T = t.asInstanceOf[T]
  }

}

package object caseapp {

  type ExtraName = Name

  type @@[T, Tag] = scalaz.@@[T, Tag]
  val Tag = scalaz.Tag

}

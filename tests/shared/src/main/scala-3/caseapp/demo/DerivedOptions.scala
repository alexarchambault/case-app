package caseapp.demo

import caseapp.*

@AppName("Derived app")
final case class DerivedOptions(
  @ExtraName("n") num: Int = 2,
  values: List[String]
) derives Parser, Help

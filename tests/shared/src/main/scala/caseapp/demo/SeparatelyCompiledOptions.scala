package caseapp.demo

import caseapp._

/** Compiled separately from the tests using it, so that derivation there reads it from the class
  * path rather than from the sources being compiled
  */
final case class SeparatelyCompiledOptions(
  @ExtraName("a") @ExtraName("b") first: Int = 1,
  required: String,
  @Tag("z") @Tag("a") third: Int = 3
)

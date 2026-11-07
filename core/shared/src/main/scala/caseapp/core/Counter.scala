package caseapp.core

/** Helper to count how many times a flag argument is specified.
  *
  * Should be used with `Int` and [[caseapp.@@]], like `Int @@ Counter`. Each occurrence of the
  * corresponding option increments the counter. Such fields default to 0 when not specified (see
  * [[caseapp.core.default.Default]]).
  *
  * {{{
  * case class Options(
  *   @Name("v") verbose: Int @@ Counter
  * )
  * // "-v -v -v" gives a verbose value of 3, accessible with options.verbose.value
  * }}}
  */
sealed abstract class Counter

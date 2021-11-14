package caseapp.core

/** Helper to count how many times a flag argument is specified.
  *
  * Should be used with [[Int]] and [[caseapp.@@]], like `Int @@ Counter`.
  */
sealed abstract class Counter

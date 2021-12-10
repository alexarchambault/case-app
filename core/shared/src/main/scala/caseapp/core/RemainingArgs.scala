package caseapp.core

import dataclass.data

/** Arguments that don't correspond to options.
  *
  * @param remaining:
  *   arguments before any `--`
  * @param unparsed:
  *   arguments after a first `--`, if any
  */
@data class RemainingArgs(
  indexedRemaining: Seq[Indexed[String]],
  indexedUnparsed: Seq[Indexed[String]]
) {

  def remaining: Seq[String] = indexedRemaining.map(_.value)
  def unparsed: Seq[String]  = indexedUnparsed.map(_.value)

  /** Arguments both before and after a `--`.
    *
    * The first `--`, if any, is not included in this list.
    */
  def all: Seq[String] =
    remaining ++ unparsed
}

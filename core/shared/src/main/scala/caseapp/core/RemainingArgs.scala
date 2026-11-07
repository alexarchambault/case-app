package caseapp.core

import dataclass.data

/** Arguments that don't correspond to options.
  *
  * Passed to [[caseapp.core.app.CaseApp.run]], and returned by
  * [[caseapp.core.app.CaseApp.detailedParse]]. For example, with `--foo 1 a -- b --bar`, if `--foo`
  * is an option, [[remaining]] is `Seq("a")`, and [[unparsed]] is `Seq("b", "--bar")`.
  *
  * @param indexedRemaining
  *   arguments before any `--`, along with their position in the arguments
  * @param indexedUnparsed
  *   arguments after a first `--`, if any, along with their position in the arguments
  */
@data(
  deprecatedSetters = true,
  deprecatedSettersMessage = "Use copy instead",
  deprecatedSettersSince = "2.1.2"
) case class RemainingArgs(
  indexedRemaining: Seq[Indexed[String]],
  indexedUnparsed: Seq[Indexed[String]]
) {

  /** Arguments before any `--`
    *
    * Unrecognized options are there too, if they were ignored (see
    * [[caseapp.core.app.CaseApp.ignoreUnrecognized]]). If parsing stopped at the first unrecognized
    * argument (see [[caseapp.core.app.CaseApp.stopAtFirstUnrecognized]]), this contains all the
    * arguments from it, including any `--`.
    */
  lazy val remaining: Seq[String] = indexedRemaining.map(_.value)

  /** Arguments after a first `--`, if any (these aren't parsed, even if they look like options) */
  lazy val unparsed: Seq[String] = indexedUnparsed.map(_.value)

  /** Arguments both before and after a `--`.
    *
    * The first `--`, if any, is not included in this list.
    */
  lazy val all: Seq[String] =
    remaining ++ unparsed

  /** Like [[all]], along with the position of each argument */
  lazy val indexed: Seq[Indexed[String]] =
    indexedRemaining ++ indexedUnparsed
}

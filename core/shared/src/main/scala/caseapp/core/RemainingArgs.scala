package caseapp.core

/**
  * Arguments that don't correspond to options.
  *
  * @param remaining: arguments before any `--`
  * @param unparsed: arguments after a first `--`, if any
  */
final case class RemainingArgs(
  remaining: Seq[String],
  unparsed: Seq[String]
) {

  /**
    * Arguments both before and after a `--`.
    *
    * The first `--`, if any, is not included in this list.
    */
  def all: Seq[String] =
    remaining ++ unparsed
}

package caseapp

final case class RemainingArgs(
  /** Arguments not corresponding to options, before any `--` */
  remainingArgs: Seq[String],
  /** Arguments not corresponding to options, after the first `--`, if any */
  unparsedArgs: Seq[String]
) {

  /**
    * Arguments not corresponding to options, both before and after a `--`.
    *
    * The first `--`, if any, is not included in this list.
    */
  lazy val args: Seq[String] =
    remainingArgs ++ unparsedArgs
}

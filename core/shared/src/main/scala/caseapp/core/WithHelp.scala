package caseapp
package core

case class WithHelp[T](
  @HelpMessage("Print usage and exit")
    usage: Boolean = false,
  @HelpMessage("Print help message and exit")
  @ExtraName("h")
    help: Boolean = false,
  @Recurse
    base: T
) {
  def map[U](f: T => U): WithHelp[U] =
    copy(base = f(base))
}

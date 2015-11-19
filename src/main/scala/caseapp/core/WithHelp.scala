package caseapp
package core

case class WithHelp[T](
  usage: Boolean = false,
  @ExtraName("h") help: Boolean = false,
  @Recurse base: T
) {
  def map[U](f: T => U): WithHelp[U] =
    copy(base = f(base))
}

package caseapp
package internals

import scala.util.Try

case class ArgDescription(options: List[String], valueNameOption: Option[String], descriptions: List[String])

trait Folder[T] {
  def descriptions: List[ArgDescription]
  def apply(current: T, args: List[String]): Try[Option[(T, List[String])]]
}

object Folder {
  def apply[T](desc: List[ArgDescription])(f: (T, List[String]) => Try[Option[(T, List[String])]]): Folder[T] = new Folder[T] {
    def descriptions = desc
    def apply(current: T, args: List[String]) = f(current, args)
  }
}

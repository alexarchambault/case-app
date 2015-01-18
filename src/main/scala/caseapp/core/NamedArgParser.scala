package caseapp
package core

import scala.util.Try

case class NamesInfo(names: List[String], isFlag: Boolean)

/**
 * Parser for `T`
 */
trait NamedArgParser[T] {
  def namesInfos: List[NamesInfo]

  /**
   * Given the current value of this arg, and the next arguments, returns either:
   *  - a Failure if some invalid arguments were found,
   *  - Success(None) if no invalid arguments were found, neither a value for this argument,
   *  - or Success(Some(value, remainingArgs)) if a value `value` for this argument was found
   */
  def apply(current: T, args: List[String]): Try[Option[(T, List[String])]]
}

object NamedArgParser {
  def from[T](namesInfos0: List[NamesInfo])(f: (T, List[String]) => Try[Option[(T, List[String])]]): NamedArgParser[T] = new NamedArgParser[T] {
    def namesInfos = namesInfos0
    def apply(current: T, args: List[String]) = f(current, args)
  }
}

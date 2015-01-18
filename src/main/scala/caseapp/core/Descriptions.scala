package caseapp
package core

import reflect.runtime.universe.TypeTag
import shapeless.{ :+:, CNil, Inl, Inr }
import caseapp.core.util._

object Descriptions {

  // From scopt
  val NL = System.getProperty("line.separator")
  val WW = "  "
  val TB = "        "

}

/**
 * Description of each of the members of a case class, and possibly of its nested case classes
 */
case class Descriptions(descriptions: List[(String, Either[Descriptions, (List[Name], List[ValueDescription], List[HelpMessage])])]) {
  import Descriptions._

  def allOptions: List[(String, (List[Name], List[ValueDescription], List[HelpMessage]))] = {
    def helper(d: Descriptions): List[(String, (List[Name], List[ValueDescription], List[HelpMessage]))] =
      d.descriptions.flatMap {
        case (name, Right(lists)) =>
          List(name -> lists)
        case (name, Left(desc)) =>
          helper(desc)
      }

    helper(this)
  }

  def usageMessage(appName: String, argsNameOption: Option[String]): String =
    s"Usage: $appName [options] ${argsNameOption.map("<" + _ + ">").mkString}"

  def optionsMessage(optionDescriptions: Map[String, NamesInfo]): String =
    allOptions.map {
      case (name, (_names, _values, messages)) =>
        val names = Name(name) :: _names
        // FIXME Flags that accept no value are not given the right help message here
        val values = if (_values.isEmpty && optionDescriptions.get(names.head.option).forall(!_.isFlag)) List(ValueDescription("value")) else _values

        val usage = s"$WW${names.map(_.option) mkString " | "}  ${values.map(_.message) mkString " | "}"
        val message = Some(messages).filter(_.nonEmpty).fold(List.empty[String])(m => List(m.map(TB + _.message) mkString NL))

        (usage :: message) mkString NL
    } .mkString(NL)
}

/**
 * Type class providing a `Descriptions` for `T`
 */
sealed trait DescriptionsOf[T] {
  def apply(): Descriptions
}

object DescriptionsOf {
  def apply[T](implicit descriptionsOf: DescriptionsOf[T]): DescriptionsOf[T] = descriptionsOf

  def fromCCAnnotations(annotation: CCRecursiveFieldAnnotations[Name :+: ValueDescription :+: HelpMessage :+: CNil]): Descriptions = Descriptions(
    annotation.annotations.map{
      case (name, Left(rec)) =>
        name -> Left(fromCCAnnotations(rec))
      case (name, Right(l)) =>
        name -> Right((l.collect{case Inl(v) => v}, l.collect{case Inr(Inl(v)) => v}, l.collect{case Inr(Inr(Inl(v))) => v}))
    }
  )

  def from[T](descriptions: => Descriptions): DescriptionsOf[T] = new DescriptionsOf[T] {
    def apply() = descriptions
  }

  // FIXME This should use macros instead of reflection
  implicit def ccDescriptionsOf[CC <: Product : TypeTag]: DescriptionsOf[CC] = DescriptionsOf.from {
    fromCCAnnotations(
      ccRecursiveMembersAnnotationsFold(util.instantiateThreeCCAnnotationFromAnnotation[Name, ValueDescription, HelpMessage])
    )
  }
}

package caseapp
package internals

import scala.reflect.runtime.universe.TypeTag
import shapeless.{ :+:, CNil, Inl, Inr }
import caseapp.internals.util._

case class RecNames(names: List[(String, Either[RecNames, List[Name]])]) {
  def withDefaults: RecNames = RecNames(
    names.map{
      case (name, Left(rec)) =>
        name -> Left(rec.withDefaults)
      case (name, Right(names)) =>
        name -> Right(Name(name) :: names)
    }
  )
}

object RecNames {
  def fromCCAnnotations(ann: CCRecursiveFieldAnnotations[Name]): RecNames =
    RecNames(ann.annotations.map{
      case (name, Right(names)) => name -> Right(names)
      case (name, Left(recAnn)) => name -> Left(fromCCAnnotations(recAnn))
    })
}

sealed trait RecNamesOf[T] {
  def apply(): RecNames
}

object RecNamesOf {
  
  def apply[T](names: => RecNames): RecNamesOf[T] = new RecNamesOf[T] {
    def apply() = names
  }

  implicit def recNamesOfCC[CC <: Product : TypeTag]: RecNamesOf[CC] = RecNamesOf {
    RecNames.fromCCAnnotations(
      ccRecursiveMembersAnnotationsFold(util.instantiateCCAnnotationFromAnnotation[Name])
    ).withDefaults
  }
  
}

object Descriptions {

  // From scopt
  val NL = System.getProperty("line.separator")
  val WW = "  "
  val TB = "        "

}


case class Descriptions(descriptions: List[(String, Either[Descriptions, (List[Name], List[ValueDescription], List[HelpMessage])])]) {
  import Descriptions._

  def allOptions: List[(String, (List[Name], List[ValueDescription], List[HelpMessage]))] = {
    def helper(d: Descriptions): List[(String, (List[Name], List[ValueDescription], List[HelpMessage]))] =
      d.descriptions.flatMap {
        case (name, Right(lists)) =>
          List(name -> lists)
        case (name, Left(recDesc)) =>
          helper(recDesc)
      }

    helper(this)
  }

  def usageMessage(appName: String, argsNameOption: Option[String]): String =
    s"Usage: $appName [options] ${argsNameOption.map("<" + _ + ">").mkString}"

  def optionsMessage(optionDescriptions: Map[String, ArgDescription]): String =
    allOptions.map {
      case (name, (_names, _values, messages)) =>
        val names = Name(name) :: _names
        // FIXME Flags that accept no value are not given the right help message here
        val values = if (_values.isEmpty) optionDescriptions.get(names.head.option).flatMap(d => d.valueNameOption).map(ValueDescription(_)).toList else _values

        val usage = s"$WW${names.map(_.option) mkString " | "}  ${values.map(_.message) mkString " | "}"
        val message =
          if (messages.isEmpty)
            None
          else {
            Some(messages.map(_.message).map(TB + _) mkString NL)
          }

        (usage :: message.toList) mkString NL
    } .mkString(NL)
}

sealed trait DescriptionsOf[T] {
  def apply(): Descriptions
}

object DescriptionsOf {

  def fromCCAnnotations(annotation: CCRecursiveFieldAnnotations[Name :+: ValueDescription :+: HelpMessage :+: CNil]): Descriptions = Descriptions(
    annotation.annotations.map{
      case (name, Left(rec)) =>
        name -> Left(fromCCAnnotations(rec))
      case (name, Right(l)) =>
        name -> Right((l.collect{case Inl(v) => v}, l.collect{case Inr(Inl(v)) => v}, l.collect{case Inr(Inr(Inl(v))) => v}))
    }
  )

  def apply[T](descriptions: => Descriptions): DescriptionsOf[T] = new DescriptionsOf[T] {
    def apply() = descriptions
  }

  implicit def ccDescriptionsOf[CC <: Product : TypeTag]: DescriptionsOf[CC] = DescriptionsOf {
    fromCCAnnotations(
      ccRecursiveMembersAnnotationsFold(util.instantiateThreeCCAnnotationFromAnnotation[Name, ValueDescription, HelpMessage])
    )
  }

}

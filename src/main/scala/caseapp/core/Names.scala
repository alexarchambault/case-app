package caseapp
package core

import reflect.runtime.universe.TypeTag
import caseapp.core.util._

/**
 * Names of each of the members of a case class, and possibly of its nested case classes
 */
case class Names(names: List[(String, Either[Names, List[Name]])]) {
  def withDefaults: Names = Names(
    names.map{
      case (name, Left(names)) =>
        name -> Left(names.withDefaults)
      case (name, Right(nameList)) =>
        name -> Right(Name(name) :: nameList)
    }
  )
}

object Names {
  def fromCCAnnotations(ann: CCRecursiveFieldAnnotations[Name]): Names =
    Names(ann.annotations.map{
      case (name, Right(names)) => name -> Right(names)
      case (name, Left(recAnn)) => name -> Left(fromCCAnnotations(recAnn))
    })
}

/**
 * Type class providing a `Names` for `T`
 */
sealed trait NamesOf[T] {
  def apply(): Names
}

object NamesOf {
  def apply[T](implicit namesOf: NamesOf[T]): NamesOf[T] = namesOf
  
  def from[T](names: => Names): NamesOf[T] = new NamesOf[T] {
    def apply() = names
  }

  // FIXME This should use macros instead of reflection
  implicit def namesOfCC[CC <: Product : TypeTag]: NamesOf[CC] = NamesOf.from {
    Names.fromCCAnnotations(
      ccRecursiveMembersAnnotationsFold(util.instantiateCCAnnotationFromAnnotation[Name])
    ).withDefaults
  }
}

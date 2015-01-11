package caseapp
package internals

import java.util.GregorianCalendar

import reflect._
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe.{ Try => _, Name => _, _ }
import shapeless.{ :+:, CNil, Inl, Inr }

package object util {

  // See http://stackoverflow.com/questions/15095727/in-scala-2-10-how-do-you-create-a-classtag-given-a-typetag
  // See http://stackoverflow.com/a/14034802/3714539

  def classTagOf[T: TypeTag]: ClassTag[T] =
    ClassTag[T](runtimeMirror(getClass.getClassLoader) runtimeClass typeTag[T].tpe)

  def classAnnotationsFold[C : TypeTag, A](f: Annotation => Option[A]): List[A] =
    (typeOf[C].typeSymbol.asClass.annotations :\ List.empty[A]) { (ann, acc) =>
      f(ann) match {
        case Some(a) =>
          a :: acc
        case None =>
          acc
      }
    }

  case class CCRecursiveFieldAnnotations[A](annotations: List[(String, Either[CCRecursiveFieldAnnotations[A], List[A]])])

  /**
   * See http://stackoverflow.com/questions/14785054/construct-case-class-from-collection-of-parameters
   * and http://stackoverflow.com/questions/16079113/scala-2-10-reflection-how-do-i-extract-the-field-values-from-a-case-class
   */

  def ccRecursiveMembersAnnotations[C <: Product : TypeTag, A <: Product : TypeTag]: CCRecursiveFieldAnnotations[A] =
    ccRecursiveMembersAnnotationsHelper(typeOf[C])

  private def ccRecursiveMembersAnnotationsHelper[A <: Product : TypeTag](tpe: Type): CCRecursiveFieldAnnotations[A] =
    ccRecursiveMembersAnnotationsFoldHelper(tpe, util.instantiateCCAnnotationFromAnnotation)

  def ccRecursiveMembersAnnotationsFold[C <: Product : TypeTag, A](f: Annotation => Option[A]): CCRecursiveFieldAnnotations[A] =
    ccRecursiveMembersAnnotationsFoldHelper(typeOf[C], f)

  private def ccRecursiveMembersAnnotationsFoldHelper[A](tpe: Type, f: Annotation => Option[A]): CCRecursiveFieldAnnotations[A] = {
    val constructorSymbol = tpe.declaration(nme.CONSTRUCTOR) // FIXME Should be tpe.decl in scala 2.11
    val defaultConstructor =
      if (constructorSymbol.isMethod) constructorSymbol.asMethod
      else
        constructorSymbol.asTerm.alternatives.map {
          _.asMethod
        }.find {
          _.isPrimaryConstructor
        }.get

    CCRecursiveFieldAnnotations(
      defaultConstructor.paramss.flatten.zipWithIndex.map { case (sym, idx) =>
        val t =
          for {
            t <- Some(tpe.member(sym.name)).filter(_.isMethod)
            _type <- Some(t.asMethod.typeSignatureIn(tpe).typeSymbol).filter(_.isClass) // FIXME Add a .finalResultType before .typeSymbol in scala 2.11
            c <- Some(_type.asClass).filter(_.isCaseClass)
          } yield c.typeSignature

        sym.name.toString -> (t match {
          case Some(c) =>
            Left(ccRecursiveMembersAnnotationsFoldHelper[A](c, f))
          case None =>
            val allAnnotations = tpe.member(sym.name).annotations
            val annotations = allAnnotations.map(f).collect { case Some(v) => v}
            Right(annotations)
        })
      }
    )
  }

  def ccMembersAnnotations[C <: Product : TypeTag, A <: Product : TypeTag]: List[(String, List[A])] = {
    val tpe = typeOf[C]

    val constructorSymbol = tpe.declaration(nme.CONSTRUCTOR)
    val defaultConstructor =
      if (constructorSymbol.isMethod) constructorSymbol.asMethod
      else
        constructorSymbol.asTerm.alternatives.map {
          _.asMethod
        }.find {
          _.isPrimaryConstructor
        }.get

    val annotationBuild = util.instantiateCCAnnotationFromAnnotation[A]

    defaultConstructor.paramss.flatten.zipWithIndex.map { case (sym, idx) =>
      val allAnnotations = tpe.member(sym.name).annotations
      val annotations = allAnnotations.map(annotationBuild).collect { case Some(v) => v}

      sym.name.toString -> annotations
    }
  }


  def instantiateCCWithDefaultValues[C <: Product : TypeTag]: C = {
    val _class = currentMirror.classSymbol(classTagOf[C].runtimeClass)
    val module = currentMirror.reflectModule(_class.companionSymbol.asModule)
    val im = currentMirror reflect module.instance

    instantiateCCWithDefaultValuesHelper[C](im)
  }

  private def instantiateCCWithDefaultValuesHelper[C <: Product](im: InstanceMirror): C = {
    val typeSignature = im.symbol.typeSignature
    val method = typeSignature.member(newTermName("apply")).asMethod

    val args = method.paramss.flatten.zipWithIndex .map {case (p, idx) =>
      typeSignature.member(newTermName("apply$default$" + (idx+1).toString)) match {
        case NoSymbol =>
          p.typeSignature match {
            case t if t =:= typeOf[Boolean]         => false
            case t if t =:= typeOf[String]          => ""
            case t if t =:= typeOf[Int]             => 0
            case t if t <:< typeOf[Option[Any]]     => None
            case t if t <:< typeOf[List[Any]]     => Nil
            case t if t =:= typeOf[Long]             => 0L
            case t if t =:= typeOf[Float]             => 0.0f
            case t if t =:= typeOf[Double]             => 0.0
            case t if t =:= typeOf[java.util.Calendar]  => new GregorianCalendar()
            case t                         => throw new IllegalArgumentException(t.toString)
          }

        case defaultArg =>
          im.reflectMethod(defaultArg.asMethod)()
      }
    }

    instantiateCCHelper(im)(args)
  }

  private def instantiateCCHelper[C <: Product](im: InstanceMirror): Seq[Any] => C = {
    val typeSignature = im.symbol.typeSignature
    val method = typeSignature.member(newTermName("apply")).asMethod

    im.reflectMethod(method)(_: _*).asInstanceOf[C]
  }

  def instantiateCC[C <: Product : TypeTag]: Seq[Any] => C = {
    val _class = currentMirror.classSymbol(classTagOf[C].runtimeClass)
    val module = currentMirror.reflectModule(_class.companionSymbol.asModule)
    val im = currentMirror reflect module.instance

    instantiateCCHelper[C](im)
  }

  // FIXME Lots of deprecated methods in scala 2.11 in the three functions below

  def instantiateCCAnnotationFromAnnotation[C <: Product : TypeTag]: Annotation => Option[C] = {
    val c = util.instantiateCC[C]
    val tpe = typeOf[C]

    a: Annotation =>
      if (a.tpe =:= tpe)
        Some(c(a.scalaArgs.map(a => a.productElement(0).asInstanceOf[Constant].value)))
      else
        None
  }

  def instantiateTwoCCAnnotationFromAnnotation[C <: Product : TypeTag, D <: Product : TypeTag]: Annotation => Option[Either[D, C]] = {
    val c = util.instantiateCC[C]
    val cTpe = typeOf[C]

    val d = util.instantiateCC[D]
    val dTpe = typeOf[D]

    a: Annotation =>
      if (a.tpe =:= cTpe)
        Some(Right(c(a.scalaArgs.map(a => a.productElement(0).asInstanceOf[Constant].value))))
      else if (a.tpe =:= dTpe)
        Some(Left(d(a.scalaArgs.map(a => a.productElement(0).asInstanceOf[Constant].value))))
      else
        None
  }

  def instantiateThreeCCAnnotationFromAnnotation[C <: Product : TypeTag, D <: Product : TypeTag, E <: Product : TypeTag]: Annotation => Option[C :+: D :+: E :+: CNil] = {
    val c = util.instantiateCC[C]
    val cTpe = typeOf[C]

    val d = util.instantiateCC[D]
    val dTpe = typeOf[D]

    val e = util.instantiateCC[E]
    val eTpe = typeOf[E]

    a: Annotation =>
      if (a.tpe =:= cTpe)
        Some(Inl(c(a.scalaArgs.map(a => a.productElement(0).asInstanceOf[Constant].value))))
      else if (a.tpe =:= dTpe)
        Some(Inr(Inl(d(a.scalaArgs.map(a => a.productElement(0).asInstanceOf[Constant].value)))))
      else if (a.tpe =:= eTpe)
        Some(Inr(Inr(Inl(e(a.scalaArgs.map(a => a.productElement(0).asInstanceOf[Constant].value))))))
      else
        None
  }

  def pascalCaseSplit(s: List[Char]): List[String] =
    if (s.isEmpty)
      Nil
    else if (!s.head.isUpper) {
      val (w, tail) = s.span(!_.isUpper)
      w.mkString :: pascalCaseSplit(tail)
    } else if (s.tail.headOption.forall(!_.isUpper)) {
      val (w, tail) = s.tail.span(!_.isUpper)
      (s.head :: w).mkString :: pascalCaseSplit(tail)
    } else {
      val (w, tail) = s.span(_.isUpper)
      if (tail.isEmpty)
        w.mkString :: pascalCaseSplit(tail)
      else
        w.init.mkString :: pascalCaseSplit(w.last :: tail)
    }

}

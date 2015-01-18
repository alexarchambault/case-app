package caseapp
package core

import reflect.runtime.universe.{ TypeTag, typeTag }

/**
 * Provides usage and help messages related to `T`
 */
case class Messages[T : DescriptionsOf : TypeTag : NamesOf : ArgParser]() {
  private val _parser = ArgParser[T].apply(Left(NamesOf[T].apply()))
  private val desc = _parser.namesInfos.flatMap{
    case desc @ NamesInfo(options, _) => options.map(_ -> desc)
  }.toMap

  private def typeName = typeTag[T].tpe.typeSymbol.name.decodedName.toString

  private val appName = {
    val n = (AppName("") /: util.classAnnotationsFold[T, AppName](util.instantiateCCAnnotationFromAnnotation[AppName]))(_ orElse _).appName
    if (n.nonEmpty)
      n
    else
      typeName
  }
  private val appVersion = {
    val n = (AppVersion("") /: util.classAnnotationsFold[T, AppVersion](util.instantiateCCAnnotationFromAnnotation[AppVersion]))(_ orElse _).appVersion
    if (n.nonEmpty)
      n
    else
      ""
  }
  private val progName = {
    val n = (ProgName("") /: util.classAnnotationsFold[T, ProgName](util.instantiateCCAnnotationFromAnnotation[ProgName]))(_ orElse _).progName
    if (n.nonEmpty)
      n
    else
      util.pascalCaseSplit(typeName.toList).map(_.toLowerCase).mkString("-")
  }
  private val argsNameOptions = {
    val n = (ArgsName("") /: util.classAnnotationsFold[T, ArgsName](util.instantiateCCAnnotationFromAnnotation[ArgsName]))(_ orElse _).argsName
    Some(n).filter(_.nonEmpty)
  }

  def usageMessage = DescriptionsOf[T].apply().usageMessage(progName, argsNameOptions)
  def helpMessage = {
    val d = DescriptionsOf[T].apply()

    val b = new StringBuilder
    b ++= s"$appName $appVersion${Descriptions.NL}"
    b ++= d.usageMessage(progName, argsNameOptions)
    b ++= Descriptions.NL
    b ++= d.optionsMessage(desc)
    b ++= Descriptions.NL
    b.result()
  }
}

object Messages {
  implicit def messages[T : DescriptionsOf : TypeTag : NamesOf : ArgParser]: Messages[T] = Messages[T]()
}

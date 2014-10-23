package caseapp

import scala.reflect.runtime.universe.TypeTag
import caseapp.internals._

case class Messages[T : DescriptionsOf : TypeTag : RecNamesOf : PreFolder]() {

  private val folder = implicitly[PreFolder[T]].apply(Left(implicitly[RecNamesOf[T]].apply()))
  private val desc = folder.descriptions.flatMap{
    case desc @ ArgDescription(options, _, _) => options.map(_ -> desc)
  }.toMap

  private def typeName = implicitly[TypeTag[T]].tpe.typeSymbol.name.decodedName.toString

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

  def usageMessage = implicitly[DescriptionsOf[T]].apply().usageMessage(progName, argsNameOptions)
  def helpMessage = {
    val d = implicitly[DescriptionsOf[T]].apply()

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
  implicit def messages[T : DescriptionsOf : TypeTag : RecNamesOf : PreFolder]: Messages[T] = Messages[T]()
}

case class WithHelp[T <: ArgsApp : DescriptionsOf : TypeTag : RecNamesOf : PreFolder](
                  base: T
, @ExtraName("h") help: Boolean = false
,                usage: Boolean = false
) extends ArgsApp {

  private val messages = Messages[T]()

  private[caseapp] def setRemainingArgs(remainingArgs: Seq[String]): Unit = {
    base.setRemainingArgs(remainingArgs)
  }
  def remainingArgs: Seq[String] = base.remainingArgs
  def apply(): Unit = {
    if (help) {
      Console.println(messages.helpMessage)
      sys exit 0
    } else if (usage) {
      Console.println(messages.usageMessage)
      sys exit 0
    } else
      base()
  }
}

object WithHelp {
  
  implicit def default[T <: ArgsApp : Default : DescriptionsOf : TypeTag : RecNamesOf : PreFolder]: Default[WithHelp[T]] = Default {
    WithHelp(
      base = implicitly[Default[T]].apply()
    , help = false
    , usage = false
    )
  } 
  
}

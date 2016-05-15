package caseapp

package object core {

  implicit class NameOps(name: Name) {

    private def isShort = name.name.length == 1

    private def optionName = caseapp.core.util.pascalCaseSplit(name.name.toList).map(_.toLowerCase).mkString("-")
    private def optionEq = if (isShort) s"-${name.name}=" else s"--$optionName="

    def option: String = if (isShort) s"-${name.name}" else s"--$optionName"

    def apply(args: List[String], isFlag: Boolean): Option[List[String]] = args match {
      case Nil => None
      case h :: t =>
        if (h == option)
          Some(t)
        else if (!isFlag && h.startsWith(optionEq))
          Some(h.drop(optionEq.length) :: t)
        else
          None
    }

    def apply(arg: String): Either[Unit, Option[String]] =
      if (arg == option)
        Right(None)
      else if (arg.startsWith(optionEq))
        Right(Some(arg.drop(optionEq.length)))
      else
        Left(())
  }

  implicit class ValueDescriptionOps(desc: ValueDescription) {

    def message: String = s"<${desc.description}>"
  }

}

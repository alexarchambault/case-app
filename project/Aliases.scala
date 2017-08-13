
import sbt._
import sbt.Keys._

object Aliases {

  def libs = libraryDependencies

  def root = file(".")

  def aliases(nameCommand: (String, String)*) =
    nameCommand.flatMap {
      case (name, command) =>
        addCommandAlias(name, command)
    }

  def commandSeq(command: String*) =
    command.mkString(";", ";", "")


  implicit class ProjectOps(val proj: Project) extends AnyVal {
    def underDoc: Project = {
      val base = proj.base
      proj.in(base.getParentFile / "doc" / base.getName)
    }
  }

}
package caseapp

import java.nio.file.Paths

object TestResources {
  def path(name: String): String =
    Paths.get(TestResources.getClass.getResource(s"/$name").toURI).toString
}

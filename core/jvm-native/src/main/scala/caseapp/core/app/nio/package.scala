package caseapp.core.app

package object nio {

  type Path = java.nio.file.Path

  object Paths {
    def get(path: String): Path =
      java.nio.file.Paths.get(path)
  }

  object File {
    def separator: String =
      java.io.File.separator
  }

}

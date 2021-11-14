package caseapp.core.parser

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file._

object PlatformArgsExpander {

  def expand(args: List[String]): List[String] =
    args.flatMap { arg =>
      if (arg.startsWith("@")) {
        val argPath = Paths.get(arg.substring(1))
        val argText = new String(Files.readAllBytes(argPath), UTF_8)
        argText.split(System.lineSeparator).map(_.trim).filter(_.nonEmpty).toList
      }
      else
        List(arg)
    }
}

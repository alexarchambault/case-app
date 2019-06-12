package caseapp.core.argparser

import java.nio.file.{InvalidPathException, Path, Paths}
import java.text.{ParseException, SimpleDateFormat}
import java.util.{Calendar, GregorianCalendar}

import caseapp.core.Error

abstract class PlatformArgParsers {

  implicit val path: ArgParser[Path] = {
    SimpleArgParser.from("path/to/file") { pathString =>
      try {
        Right(Paths.get(pathString))
      } catch {
        case e: InvalidPathException =>
          Left(Error.MalformedValue("path", e.getMessage))
      }
    }
  }

  implicit val calendar: ArgParser[Calendar] =
    SimpleArgParser.from("yyyy-MM-dd") { s =>
      val c = new GregorianCalendar

      try {
        c.setTime(PlatformArgParsers.fmt.parse(s))
        Right(c)
      } catch {
        case e: ParseException =>
          Left(Error.MalformedValue("date", Option(e.getMessage).getOrElse("")))
      }
    }

}

object PlatformArgParsers {

  private val fmt = new SimpleDateFormat("yyyy-MM-dd")

}

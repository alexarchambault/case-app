package caseapp.core.argparser

import java.text.{ParseException, SimpleDateFormat}
import java.util.{Calendar, GregorianCalendar}

import caseapp.core.Error

abstract class PlatformArgParsers {

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
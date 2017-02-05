package caseapp.core

import java.text.{ SimpleDateFormat, ParseException }
import java.util.{ Calendar, GregorianCalendar }

trait PlatformArgParsers {

  implicit def calendar: ArgParser[Calendar] =
    ArgParser.instance("yyyy-MM-dd") {
      val fmt = new SimpleDateFormat("yyyy-MM-dd")

      s =>
        try {
          val c = new GregorianCalendar
          c setTime fmt.parse(s)
          Right(c)
        } catch { case e: ParseException =>
          Left(s"Cannot parse date: $s")
        }
    }

}

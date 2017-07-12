package caseapp

import java.util.{Calendar, GregorianCalendar}
import utest._

object PlatformTests extends TestSuite {

  final case class WithCalendar(
    date: Calendar
  )

  CaseApp.parseWithHelp[WithCalendar] _

  val tests = TestSuite {
    "parse a date" - {
      val res = Parser[WithCalendar].parse(Seq("--date", "2014-10-23"))
      val expectedRes = Right((
        WithCalendar(date = new GregorianCalendar(2014, 9, 23)),
        Nil
      ))
      assert(res == expectedRes)
    }
  }

}

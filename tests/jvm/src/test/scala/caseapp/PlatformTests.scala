package caseapp

import java.util.GregorianCalendar

import org.scalatest.{Matchers, PropSpec}

object PlatformTests {

  final case class WithCalendar(
    date   : java.util.Calendar
  )

  CaseApp.parseWithHelp[WithCalendar] _

}

class PlatformTests extends PropSpec with Matchers {
  import PlatformTests._

  property("parse a date") {
    Parser[WithCalendar].parse(Seq("--date", "2014-10-23")) shouldEqual Right((WithCalendar(date = {
      new GregorianCalendar(2014, 9, 23)
    }), Seq.empty))
  }

}

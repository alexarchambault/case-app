package caseapp

import java.util.GregorianCalendar

import org.scalatest.{ Matchers, FlatSpec }

object PlatformTests {

  case class WithCalendar(
    date   : java.util.Calendar
  ) extends App

  CaseApp.parseWithHelp[WithCalendar] _

}

class PlatformTests extends FlatSpec with Matchers {
  import PlatformTests._

  it should "parse a date" in {
    Parser[WithCalendar].parse(Seq("--date", "2014-10-23")) shouldEqual Right((WithCalendar(date = {
      new GregorianCalendar(2014, 9, 23)
    }), Seq.empty))
  }

}

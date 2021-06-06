package caseapp

import java.util.{Calendar, GregorianCalendar}
import utest._
import java.nio.file._

object PlatformTests extends TestSuite {

  final case class WithCalendar(
    date: Calendar
  )

  val withCalendarParser = CaseApp.parseWithHelp[WithCalendar] _

  final case class WithPath(
    path: Path
  )

  CaseApp.parseWithHelp[WithPath] _

  val tests = TestSuite {
    test("parse a date") {
      val res = Parser[WithCalendar].parse(Seq("--date", "2014-10-23"))
      val expectedRes = Right((
        WithCalendar(date = new GregorianCalendar(2014, 9, 23)),
        Nil
      ))
      assert(res == expectedRes)
    }

    test("parse a path") {
      val res = Parser[WithPath].parse(Seq("--path", "/path/to/file.ext"))
      val expectedRes = Right((
        WithPath(path = Paths.get("/path/to/file.ext")),
        Nil
      ))
      assert(res == expectedRes)
    }
  }

}

package caseapp

import java.util.{Calendar, GregorianCalendar}
import utest._
import java.nio.file._
import caseapp.core.Error
import caseapp.core.help.WithHelp

object PlatformTests extends TestSuite {

  final case class WithCalendar(
    date: Calendar
  )

  implicit lazy val withCalendarParser0: Parser[WithCalendar] = Parser.derive

  val withCalendarParser: Seq[String] => Either[
    caseapp.core.Error,
    (Either[Error, WithCalendar], Boolean, Boolean, Seq[String])
  ] =
    CaseApp.parseWithHelp[WithCalendar] _

  final case class WithPath(
    path: Path
  )

  // unused, but we check that this derives a Parser for WithPath
  private def checkDerivation(args: Seq[String]) =
    CaseApp.parseWithHelp[WithPath](args)

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

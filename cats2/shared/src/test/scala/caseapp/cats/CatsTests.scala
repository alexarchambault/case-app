package caseapp.cats

import _root_.cats.effect._
import _root_.cats.effect.concurrent.Ref
import _root_.cats.implicits._
import _root_.cats.data.NonEmptyList
import caseapp._
import caseapp.core.help.{CommandsHelp, Help}
import caseapp.core.Error
import utest._

import caseapp.cats.CatsArgParser._

sealed trait RecordedApp {

  val stdoutBuff: Ref[IO, List[String]] = Ref.unsafe(List.empty)
  val stderrBuff: Ref[IO, List[String]] = Ref.unsafe(List.empty)

  def run(args: List[String]): IO[ExitCode]
}

private class RecordedIOCaseApp[T](implicit parser0: Parser[T], messages: Help[T])
    extends IOCaseApp[T]()(parser0, messages) with RecordedApp {

  override def error(message: Error): IO[ExitCode] =
    stderrBuff.update(message.message :: _)
      .as(ExitCode.Error)

  override def println(x: String): IO[Unit] =
    stdoutBuff.update(x :: _)

  override def run(options: T, remainingArgs: RemainingArgs): IO[ExitCode] =
    println(s"run: $options").as(ExitCode.Success)
}

object CatsTests extends TestSuite {

  import Definitions._

  private def testCaseStdout(args: List[String], expected: String) =
    testRunFuture(
      new RecordedIOCaseApp[FewArgs](),
      args,
      expectedStdout = List(expected),
      expectedStderr = List.empty
    )

  private def testCaseStderr(args: List[String], expected: String) =
    testRunFuture(
      new RecordedIOCaseApp[FewArgs](),
      args,
      expectedStdout = List.empty,
      expectedStderr = List(expected)
    )

  private def testRunFuture(
    app: RecordedApp,
    args: List[String],
    expectedStdout: List[String],
    expectedStderr: List[String]
  ) =
    app.run(args)
      .flatMap { _ =>
        for {
          stdoutRes <- app.stdoutBuff.get
          stderrRes <- app.stderrBuff.get
        } yield assert(stdoutRes == expectedStdout, stderrRes == expectedStderr)
      }
      .unsafeToFuture()

  override def tests: Tests = Tests {
    test("IOCaseApp") {
      test("output usage") {
        testCaseStdout(List("--usage"), Help[FewArgs].withHelp.usage)
      }
      test("output help") {
        testCaseStdout(List("--help"), Help[FewArgs].withHelp.help)
      }
      test("parse error") {
        testCaseStderr(List("--invalid"), "Unrecognized argument: --invalid")
      }
      test("run") {
        testCaseStdout(List("--value", "foo", "--num-foo", "42"), "run: FewArgs(foo,42)")
      }
    }

    test("parse nonEmptyList args") {
      val res =
        Parser[WithNonEmptyList].parse(Seq("--nel", "2", "--nel", "5", "extra"))
      val expectedRes =
        Right((WithNonEmptyList(nel = NonEmptyList.of("2", "5")), Seq("extra")))
      assert(res == expectedRes)
    }
  }
}

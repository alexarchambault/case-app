package caseapp.cats

import cats.effect._
import caseapp._
import caseapp.core.help.Help
import caseapp.core.Error
import utest._

import scala.collection.mutable.ArrayBuffer

private class RecordedIOCaseApp[T](implicit parser0: Parser[T], messages: Help[T]) extends IOCaseApp[T]()(parser0, messages) {

  private var stdoutBuff = ArrayBuffer.empty[String]
  private var stderrBuff = ArrayBuffer.empty[String]

  def stdoutLog: List[String] = stdoutBuff.toList
  def stderrLog: List[String] = stderrBuff.toList

  override def error(message: Error): IO[ExitCode] = IO {
    stderrBuff += message.message
    ExitCode.Error
  }

  override def println(x: String): IO[Unit] = IO {
    stdoutBuff += x
  }

  override def run(options: T, remainingArgs: RemainingArgs): IO[ExitCode] =
    println(s"run: $options").as(ExitCode.Success)
}

object CatsTests extends TestSuite {
  import Definitions._

  private def testCheckStdout(args: List[String], expected: String) =
    testRunFuture(args, expectedStdout = List(expected), expectedStderr = List.empty)

  private def testCheckStderr(args: List[String], expected: String) =
    testRunFuture(args, expectedStdout = List.empty, expectedStderr = List(expected))

  private def testRunFuture(args: List[String], expectedStdout: List[String], expectedStderr: List[String]) = {
    val caseApp = new RecordedIOCaseApp[FewArgs]()
    caseApp.run(args)
      .flatMap { _ => IO {
        val stdoutRes = caseApp.stdoutLog
        val stderrRes = caseApp.stderrLog
        assert(stdoutRes == expectedStdout, stderrRes == expectedStderr)
      }}
      .unsafeToFuture()
  }

  override def tests: Tests = Tests {
    test("IOCaseApp") - {
      test("output usage") - {
        testCheckStdout(List("--usage"), Help[FewArgs].withHelp.usage)
      }
      test("output help") - {
        testCheckStdout(List("--help"), Help[FewArgs].withHelp.help)
      }
      test("parse error") - {
        testCheckStderr(List("--invalid"), "Unrecognized argument: --invalid")
      }
      test("run") - {
        testCheckStdout(List("--value", "foo", "--num-foo", "42"), "run: FewArgs(foo,42)")
      }
    }
  }
}

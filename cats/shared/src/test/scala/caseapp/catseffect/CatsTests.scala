package caseapp.catseffect

import cats.effect._
import cats.effect.unsafe.implicits.global
import cats.data.NonEmptyList
import caseapp._
import caseapp.core.help.Help
import caseapp.core.Error
import utest._

sealed trait RecordedApp {

  val stdoutBuff: Ref[IO, List[String]] = Ref.unsafe(List.empty)
  val stderrBuff: Ref[IO, List[String]] = Ref.unsafe(List.empty)

  def run(args: List[String]): IO[ExitCode]
}

sealed trait RecordedCommand {
  val stdoutBuff: Ref[IO, List[String]] = Ref.unsafe(List.empty)
  val stderrBuff: Ref[IO, List[String]] = Ref.unsafe(List.empty)
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

    test("IOCommandsEntryPoint") {
      def mkEntryPoint() = {
        val firstCmd = new IOCommand[Definitions.First] with RecordedCommand {
          override def names: List[List[String]]    = List(List("first"))
          override def println(x: String): IO[Unit] = stdoutBuff.update(x :: _)
          override def run(options: Definitions.First, remainingArgs: RemainingArgs): IO[ExitCode] =
            stdoutBuff.update(s"first: $options" :: _).as(ExitCode.Success)
          override def error(message: Error): IO[ExitCode] =
            stderrBuff.update(message.message :: _).as(ExitCode.Error)
        }
        val secondCmd = new IOCommand[Definitions.Second] with RecordedCommand {
          override def names: List[List[String]]    = List(List("second"))
          override def println(x: String): IO[Unit] = stdoutBuff.update(x :: _)
          override def run(
            options: Definitions.Second,
            remainingArgs: RemainingArgs
          ): IO[ExitCode] =
            stdoutBuff.update(s"second: $options" :: _).as(ExitCode.Success)
          override def error(message: Error): IO[ExitCode] =
            stderrBuff.update(message.message :: _).as(ExitCode.Error)
        }
        new IOCommandsEntryPoint {
          def progName = "test-app"
          def commands = Seq(firstCmd, secondCmd)
        }
      }

      test("dispatch to first command") {
        val app      = mkEntryPoint()
        val firstCmd = app.commands.head.asInstanceOf[RecordedCommand]
        app.run(List("first", "--foo", "hello", "--bar", "42"))
          .flatMap { code =>
            firstCmd.stdoutBuff.get.map { stdout =>
              assert(code == ExitCode.Success)
              assert(stdout == List("first: First(hello,42)"))
            }
          }
          .unsafeToFuture()
      }

      test("dispatch to second command") {
        val app       = mkEntryPoint()
        val secondCmd = app.commands(1).asInstanceOf[RecordedCommand]
        app.run(List("second", "--fooh", "world", "--baz", "7"))
          .flatMap { code =>
            secondCmd.stdoutBuff.get.map { stdout =>
              assert(code == ExitCode.Success)
              assert(stdout == List("second: Second(world,7)"))
            }
          }
          .unsafeToFuture()
      }

      test("no subcommand prints usage") {
        val app = mkEntryPoint()
        app.run(List())
          .map { code =>
            assert(code == ExitCode.Success)
          }
          .unsafeToFuture()
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

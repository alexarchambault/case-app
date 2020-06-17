---
id: cats-effect
title: Cats Effect
---

A [cats-effect](https://github.com/typelevel/cats-effect) module is available, providing
`IO` versions of the application classes referenced above. They all extend [IOApp](https://typelevel.org/cats-effect/datatypes/ioapp.html)
so [`Timer`](https://typelevel.org/cats-effect/datatypes/timer.html) and [`ContextShift`](https://typelevel.org/cats-effect/datatypes/contextshift.html)
are conveniently available.


```scala
// additional imports
import caseapp.cats._
import cats.effect._

object IOCaseExample extends IOCaseApp[ExampleOptions] {
  def run(options: ExampleOptions, arg: RemainingArgs): IO[ExitCode] = IO {
    // Core of the app
    // ...
    ExitCode.Success
  }
}

object IOCommandExample extends CommandApp[DemoCommand] {
  def run(command: DemoCommand, args: RemainingArgs): IO[ExitCode] = IO {
    // ...
    ExitCode.Success
  }
}
```

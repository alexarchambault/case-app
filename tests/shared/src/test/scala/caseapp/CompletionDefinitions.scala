package caseapp

import caseapp.core.Arg
import caseapp.core.app.CommandsEntryPoint
import caseapp.core.complete._

object CompletionDefinitions {
  object Simple {
    case class Options(value: String)
    object App extends CaseApp[Options] {
      def run(options: Options, args: RemainingArgs): Unit = ???
    }
  }

  object Multiple {
    case class Options(@Name("V") @HelpMessage("A value") value: String, @Name("n") other: Int)
    object App extends CaseApp[Options] {
      def run(options: Options, args: RemainingArgs): Unit = ???
    }
  }

  object ArgCompletion {
    case class Options(
      @Name("V") @HelpMessage("A value") value: String = "",
      @Name("n") other: Int = 0
    )
    object App extends CaseApp[Options] {
      override def completer: Completer[Options] = {
        val parent = super.completer
        new Completer[Options] {
          def optionName(prefix: String, state: Option[Options]) =
            parent.optionName(prefix, state)
          def optionValue(arg: Arg, prefix: String, state: Option[Options]) =
            if (arg.name.name == "value")
              state match {
                case None => parent.optionValue(arg, prefix, state)
                case Some(state0) =>
                  (0 to 2)
                    .map(_ + state0.other * 1000)
                    .map(n => CompletionItem(n.toString))
                    .toList
              }
            else
              parent.optionValue(arg, prefix, state)
          def argument(prefix: String, state: Option[Options]) =
            parent.argument(prefix, state)
        }
      }
      def run(options: Options, args: RemainingArgs): Unit = ???
    }
  }

  object Commands {
    case class FirstOptions(
      @Name("V") @HelpMessage("A value") value: String = "",
      @Name("n") other: Int = 0
    )
    case class SecondOptions(
      @Name("g") @HelpMessage("A pattern") glob: String = "",
      @Name("d") count: Int = 0
    )
    object First extends Command[FirstOptions] {
      def run(options: FirstOptions, args: RemainingArgs): Unit = ???
    }
    object Second extends Command[SecondOptions] {
      def run(options: SecondOptions, args: RemainingArgs): Unit = ???
    }

    object Prog extends CommandsEntryPoint {
      def progName = "prog"
      def commands = Seq(
        First,
        Second
      )
    }
  }

  object CommandsWithDefault {
    case class FirstOptions(
      @Name("V") @HelpMessage("A value") value: String = "",
      @Name("n") other: Int = 0
    )
    case class SecondOptions(
      @Name("g") @HelpMessage("A pattern") glob: String = "",
      @Name("d") count: Int = 0
    )
    object First extends Command[FirstOptions] {
      def run(options: FirstOptions, args: RemainingArgs): Unit = ???
    }
    object Second extends Command[SecondOptions] {
      def run(options: SecondOptions, args: RemainingArgs): Unit = ???
    }

    object Prog extends CommandsEntryPoint {
      def progName                = "prog"
      override def defaultCommand = Some(First)
      def commands = Seq(
        First,
        Second
      )
    }
  }
}

package caseapp.core.complete

import caseapp.core.help.{WithFullHelp, WithHelp}
import caseapp.core.{Arg, RemainingArgs}

/** Computes shell completions for the arguments of an application whose options are a `T`
  *
  * Used by [[caseapp.core.app.CaseApp.complete]], via [[caseapp.core.app.CaseApp.completer]]. The
  * default completer, [[HelpCompleter]], only completes option names. To complete option values
  * too, override `completer` in your [[caseapp.core.app.CaseApp]] or [[caseapp.core.app.Command]],
  * with [[Completer.CompleterOps.completeOptionValue]] for example:
  * {{{
  * override def completer: Completer[Options] =
  *   super.completer.completeOptionValue { (arg, prefix, state, args) =>
  *     if (arg.name.name == "color")
  *       Some(List("red", "green", "blue").filter(_.startsWith(prefix)).map(CompletionItem(_)))
  *     else
  *       None // use the completions of the underlying completer
  *   }
  * }}}
  *
  * In all methods, `prefix` is the argument being completed (possibly empty), `state` holds the
  * options parsed from the other arguments (`None` if they couldn't be parsed), and `args` holds
  * the non-option arguments.
  */
trait Completer[-T] { self =>

  /** Completes an option name (an argument starting with `-`) */
  def optionName(prefix: String, state: Option[T], args: RemainingArgs): List[CompletionItem]

  /** Completes the value of option `arg`, when passed as a separate argument (`--foo value`) */
  def optionValue(
    arg: Arg,
    prefix: String,
    state: Option[T],
    args: RemainingArgs
  ): List[CompletionItem]

  /** Completes a non-option argument */
  def argument(prefix: String, state: Option[T], args: RemainingArgs): List[CompletionItem]

  /** Completer for the arguments after `--`
    *
    * `None` by default, in which case arguments after `--` aren't completed.
    */
  def postDoubleDash(state: Option[T], args: RemainingArgs): Option[Completer[T]] =
    None

  /** Creates a `Completer[U]` out of this one, given a way to get a `T` from a `U` */
  final def contramapOpt[U](f: U => Option[T]): Completer[U] =
    Completer.Mapped(this, f)

  /** Adapts this completer to [[caseapp.core.help.WithHelp]] */
  final def withHelp: Completer[WithHelp[T]] =
    contramapOpt(_.baseOrError.toOption)

  /** Adapts this completer to [[caseapp.core.help.WithFullHelp]] */
  final def withFullHelp: Completer[WithFullHelp[T]] =
    contramapOpt(_.withHelp.baseOrError.toOption)
}

object Completer {

  /** Adds methods to [[Completer]] */
  implicit class CompleterOps[T](private val completer: Completer[T]) extends AnyVal {

    /** Creates a completer completing option values with `f`
      *
      * `f` is passed the option, the prefix, the state, and the non-option arguments (see
      * [[Completer.optionValue]]). If it returns `None`, the option value is completed by the
      * underlying completer.
      */
    def completeOptionValue(f: (
      Arg,
      String,
      Option[T],
      RemainingArgs
    ) => Option[List[CompletionItem]]): Completer[T] =
      WithOptionValue(completer, f)
  }

  /** Completer delegating everything to `default`
    *
    * Handy to override only some methods of an existing completer:
    * {{{
    * new Completer.DefaultTo(super.completer) {
    *   override def argument(prefix: String, state: Option[Options], args: RemainingArgs) =
    *     List(CompletionItem("foo"))
    * }
    * }}}
    */
  class DefaultTo[-T](default: Completer[T]) extends Completer[T] {
    override def optionName(
      prefix: String,
      state: Option[T],
      args: RemainingArgs
    ): List[CompletionItem] =
      default.optionName(prefix, state, args)
    override def optionValue(
      arg: Arg,
      prefix: String,
      state: Option[T],
      args: RemainingArgs
    ): List[CompletionItem] =
      default.optionValue(arg, prefix, state, args)
    override def argument(
      prefix: String,
      state: Option[T],
      args: RemainingArgs
    ): List[CompletionItem] =
      default.argument(prefix, state, args)
    override def postDoubleDash(state: Option[T], args: RemainingArgs): Option[Completer[T]] =
      default.postDoubleDash(state, args)

    override def toString(): String =
      s"Completer.DefaultTo($default)"
  }

  private final case class WithOptionValue[T](
    self: Completer[T],
    f: (Arg, String, Option[T], RemainingArgs) => Option[List[CompletionItem]]
  ) extends DefaultTo[T](self) {
    override def optionValue(
      arg: Arg,
      prefix: String,
      state: Option[T],
      args: RemainingArgs
    ): List[CompletionItem] =
      f(arg, prefix, state, args).getOrElse {
        super.optionValue(arg, prefix, state, args)
      }
  }

  /** Adds `extraOptionNames(prefix)` to the option names completed by `completer`
    */
  def withExtraOptionNames[T](
    completer: Completer[T],
    extraOptionNames: String => List[CompletionItem]
  ): Completer[T] =
    WithExtraOptionNames(completer, extraOptionNames)

  private final case class WithExtraOptionNames[T](
    self: Completer[T],
    extraOptionNames: String => List[CompletionItem]
  ) extends DefaultTo[T](self) {
    override def optionName(
      prefix: String,
      state: Option[T],
      args: RemainingArgs
    ): List[CompletionItem] =
      super.optionName(prefix, state, args) ++ extraOptionNames(prefix)
  }

  private final case class Mapped[T, U](self: Completer[T], f: U => Option[T])
      extends Completer[U] {
    def optionName(prefix: String, state: Option[U], args: RemainingArgs): List[CompletionItem] =
      self.optionName(prefix, state.flatMap(f), args)
    def optionValue(
      arg: Arg,
      prefix: String,
      state: Option[U],
      args: RemainingArgs
    ): List[CompletionItem] =
      self.optionValue(arg, prefix, state.flatMap(f), args)
    def argument(prefix: String, state: Option[U], args: RemainingArgs): List[CompletionItem] =
      self.argument(prefix, state.flatMap(f), args)

    override def postDoubleDash(state: Option[U], args: RemainingArgs): Option[Completer[U]] =
      self.postDoubleDash(state.flatMap(f), args).map(_.contramapOpt(f))
  }
}

package caseapp

import scala.annotation.StaticAnnotation

/** Extra name for the annotated argument
  *
  * Can be repeated, to add several extra names. The field name itself is always accepted too.
  *
  * One-character names correspond to short options (`-v`), other names to long options
  * (`--verbose`). Long names are formatted by the parser's name formatter, which by default turns
  * `camelCase` names into `kebab-case` ones (`fooBar` is accepted as `--foo-bar`).
  *
  * Also available as `caseapp.ExtraName`.
  *
  * {{{
  * case class Options(
  *   @Name("v") @Name("verb")
  *   verbose: Boolean = false // accepted as --verbose, -v, or --verb
  * )
  * }}}
  *
  * @param name
  *   extra name, without leading dashes
  */
final case class Name(name: String) extends StaticAnnotation

/** Description of the value of the annotated argument
  *
  * Shown next to the option names in help messages, like `--file path`. If this annotation is
  * absent, the description of the argument's `ArgParser` is used (like `int` or `string`).
  *
  * @param description
  *   value description, like `path` or `url`
  */
final case class ValueDescription(description: String) extends StaticAnnotation {

  /** Description wrapped in angle brackets, like `<path>` */
  def message: String = s"<$description>"
}

object ValueDescription {

  /** Value description used when none is specified, `value` */
  val default = ValueDescription("value")
}

/** Help message for the annotated argument, or for the annotated case class of arguments
  *
  * On an argument (case class field), `message` is shown next to the argument in help messages, and
  * is used as description of that option in shell completions.
  *
  * On a case class of arguments, `message` is printed after the usage line in help messages, and
  * its first line is used as description of the corresponding command in command listings and shell
  * completions.
  *
  * {{{
  * @HelpMessage("Greets people")
  * case class Options(
  *   @HelpMessage("Name of the person to greet")
  *   name: String
  * )
  * }}}
  *
  * @param message
  *   help message, as plain text
  * @param messageMd
  *   Markdown version of `message`. Not used by case-app itself, only there as a convenience for
  *   case-app users (for example to generate Markdown documentation of an application's options).
  * @param detailedMessage
  *   longer help message. On a case class of arguments, if non-empty, it replaces `message` in the
  *   full help message, printed with `--help-full` when full help is enabled (see
  *   `caseapp.core.app.CaseApp.hasFullHelp`). Ignored on arguments.
  */
final case class HelpMessage(message: String, messageMd: String = "", detailedMessage: String = "")
    extends StaticAnnotation

/** Name for the annotated case class of arguments, like `MyApp`
  *
  * If this annotation is absent, the application name is derived from the case class name, by
  * removing any `Options` suffix from it (`MyAppOptions` gives `MyApp`). It is used to compute the
  * default program name, if [[ProgName]] isn't specified.
  *
  * Stored in `caseapp.core.help.Help.appName`, it is not printed in help messages by case-app.
  */
final case class AppName(appName: String) extends StaticAnnotation

/** Program name for the annotated case class of arguments, like `my-app`
  *
  * Printed in the usage line of help messages (`Usage: my-app [options]`), and used as the default
  * command name of a `caseapp.core.app.Command`.
  *
  * If this annotation is absent, the program name is derived from the application name (see
  * [[AppName]]), by converting it to `kebab-case` (`MyApp` gives `my-app`).
  */
final case class ProgName(progName: String) extends StaticAnnotation

/** Command name for the annotated case class of arguments, like `my-command`
  *
  * Not used by case-app itself. To change the name of a `caseapp.core.app.Command`, use
  * [[ProgName]] on its options, or override its `name` or `names` methods.
  */
final case class CommandName(commandName: String) extends StaticAnnotation

/** Version of the application, for the annotated case class of arguments
  *
  * Stored in `caseapp.core.help.Help.appVersion`, it is not printed in help messages by case-app.
  */
final case class AppVersion(appVersion: String) extends StaticAnnotation

/** Name for the extra (non-option) arguments of the annotated case class of arguments
  *
  * Printed between brackets at the end of the usage line of help messages, like `Usage: my-app
  * [options] [file...]` for `@ArgsName("file...")`.
  */
final case class ArgsName(argsName: String) extends StaticAnnotation

/** Don't parse the annotated field as a single argument. Recurse on its fields instead.
  *
  * The type of the annotated field must itself be a case class of arguments (with a `Parser`). This
  * allows sharing groups of options between several applications or commands.
  *
  * Optionally, add a prefix to all the names of the fields: with `@Recurse("foo")`, a `bar` field
  * of the nested case class is accepted as `--foo-bar`.
  *
  * {{{
  * case class LoggingOptions(verbose: Boolean = false)
  * case class Options(
  *   @Recurse logging: LoggingOptions = LoggingOptions(), // accepts --verbose
  *   @Recurse("other") other: LoggingOptions = LoggingOptions() // accepts --other-verbose
  * )
  * }}}
  *
  * @param prefix
  *   prefix added to the names of the nested arguments, none if empty
  */
final case class Recurse(prefix: String) extends StaticAnnotation {

  /** Recurse on the fields of the annotated field, with no prefix */
  def this() = this("")
}

object Recurse {

  /** Recurse on the fields of the annotated field, with no prefix */
  def apply(): Recurse =
    new Recurse
}

/** Do not include this field / argument in help messages
  *
  * The argument is still accepted on the command-line. Hidden arguments are shown, marked as
  * `(hidden)`, in the full help message (printed with `--help-full` when full help is enabled, see
  * `caseapp.core.app.CaseApp.hasFullHelp`).
  */
final class Hidden extends StaticAnnotation

/** Group of the annotated argument, in help messages
  *
  * Arguments of the same group are listed together in help messages, under a `<name> options:`
  * heading. Arguments with no group are listed last, under `Other options:` (or `Options:` if no
  * argument has a group). By default, groups are sorted alphabetically. This can be customized, and
  * groups can be hidden, with `caseapp.core.help.HelpFormat`.
  *
  * The `--help` / `--usage` options added by case-app belong to the `Help` group.
  *
  * {{{
  * case class Options(
  *   @Group("Network") port: Int = 8080,
  *   @Group("Network") host: String = "localhost"
  * )
  * }}}
  *
  * @param name
  *   group name
  */
final case class Group(name: String) extends StaticAnnotation

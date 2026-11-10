/** caseapp root package
  *
  * Simply importing things directly under this package should make it possible not to have to
  * import things from [[caseapp.core]]. This package contains the annotations used to customize
  * arguments (like `HelpMessage`, `Name`, `Recurse`…), and aliases to the main types of
  * [[caseapp.core]].
  *
  * Typical usage:
  * {{{
  * import caseapp._
  *
  * case class Options(
  *   @HelpMessage("Enable verbose output")
  *   verbose: Boolean = false,
  *   @Name("n")
  *   count: Int = 1
  * )
  *
  * object MyApp extends CaseApp[Options] {
  *   def run(options: Options, remainingArgs: RemainingArgs): Unit =
  *     println("options: " + options + ", args: " + remainingArgs.all)
  * }
  * }}}
  */
package object caseapp {

  /** Alias of `caseapp.Name` */
  type ExtraName = Name

  /** Alias of `caseapp.Name` */
  val ExtraName = Name

  /** Alias of [[caseapp.core.parser.Parser]] */
  type Parser[T] = core.parser.Parser[T]

  /** Alias of [[caseapp.core.parser.Parser$ caseapp.core.parser.Parser]] */
  val Parser = core.parser.Parser

  /** Alias of [[caseapp.core.help.Help]] */
  type Help[T] = core.help.Help[T]

  /** Alias of [[caseapp.core.help.Help$ caseapp.core.help.Help]] */
  val Help = core.help.Help

  /** Alias of [[caseapp.core.app.CaseApp]] */
  type CaseApp[T] = core.app.CaseApp[T]

  /** Alias of [[caseapp.core.app.CaseApp$ caseapp.core.app.CaseApp]] */
  val CaseApp = core.app.CaseApp

  /** Alias of [[caseapp.core.app.Command]] */
  type Command[T] = core.app.Command[T]

  /** Alias of [[caseapp.core.app.CommandsEntryPoint]] */
  type CommandsEntryPoint = core.app.CommandsEntryPoint

  /** Alias of [[caseapp.core.RemainingArgs]] */
  type RemainingArgs = core.RemainingArgs

  /** Alias of [[caseapp.core.RemainingArgs$ caseapp.core.RemainingArgs]] */
  val RemainingArgs = core.RemainingArgs

  /** Alias of [[caseapp.core.argparser.Last]] */
  type Last[T] = core.argparser.Last[T]

  /** Alias of [[caseapp.core.argparser.Last]] */
  val Last = core.argparser.Last

  // Running into weird errors with this one when using Tag.of, so let's use newtype below instead
  // type @@[+T, Tag] = shapeless.tag.@@[T, Tag]
  // object Tag {
  //   def of[Tag] = shapeless.tag[Tag]
  //   def unwrap[T, Tag](t: T @@ Tag): T = t.asInstanceOf[T]
  // }

  // Custom tag implementation, see above for more details
  /** A `T` value, tagged with the type `Tag`
    *
    * Tagging a type allows to pick a different [[caseapp.core.argparser.ArgParser]] for it. Mostly
    * used as `Int @@ Counter` (see [[caseapp.core.Counter]]).
    *
    * Build values with `Tag.of[Tag](value)`, and get the underlying value back with `.value` or
    * `Tag.unwrap`.
    *
    * @param value
    *   underlying value
    */
  final case class @@[T, Tag](value: T) extends AnyVal

  /** Alias of `caseapp.annotation.Tag`, the annotation to tag arguments */
  type Tag = caseapp.annotation.Tag

  /** Helpers for the `caseapp.annotation.Tag` annotation, and for [[caseapp.@@]] tagged values
    */
  object Tag {

    /** Creates a `caseapp.annotation.Tag` annotation */
    def apply(name: String): Tag =
      caseapp.annotation.Tag(name)

    /** Extracts the name of a `caseapp.annotation.Tag` annotation */
    def unapply(tag: Tag): Option[String] =
      Some(tag.name)

    /** Tags values with `Tag`, see [[Tag.of]] */
    final class TagBuilder[Tag] {

      /** Tags `t` with `Tag` */
      def apply[T](t: T): T @@ Tag = @@(t)
    }

    /** Tags values with the type `Tag`, like `Tag.of[Counter](2)`, which gives an `Int @@ Counter`
      */
    def of[Tag]: TagBuilder[Tag] = new TagBuilder[Tag]

    /** Gets the underlying value of a tagged value */
    def unwrap[T, Tag](t: T @@ Tag): T = t.value
  }

  /** Alias of [[caseapp.core.Counter]] */
  type Counter = core.Counter

}

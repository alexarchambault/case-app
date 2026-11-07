package caseapp.annotation

import scala.annotation.StaticAnnotation

/** Arbitrary tag for the annotated argument
  *
  * Can be repeated. Tags are not used by case-app itself: they are kept in the `tags` field of the
  * `caseapp.core.Arg` describing the argument (available via `Parser#args` or `Help#args`), so that
  * users can attach their own metadata to arguments, and act on it (for example, by filtering
  * arguments in help messages with `caseapp.core.help.HelpFormat.filterArgs`).
  *
  * Also available as `caseapp.Tag`.
  *
  * {{{
  * case class Options(
  *   @Tag("experimental") foo: Boolean = false
  * )
  * }}}
  *
  * @param name
  *   tag name
  */
final case class Tag(name: String) extends StaticAnnotation

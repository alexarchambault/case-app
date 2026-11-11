package caseapp.core.complete

import dataclass._

/** A shell completion candidate
  *
  * @param value
  *   value to complete, like `--foo`
  * @param description
  *   description of the value, shown by shells that support it (zsh and fish)
  * @param extraValues
  *   other values with the same description, like aliases of an option (`-f`)
  * @param hidden
  *   whether this item corresponds to something hidden from help messages, like options marked with
  *   [[caseapp.Hidden]] or hidden commands (see [[CompletionItem.dropHidden]])
  */
@data(
  deprecatedSetters = true,
  deprecatedSettersMessage = "Use copy instead",
  deprecatedSettersSince = "2.1.2"
) case class CompletionItem(
  value: String,
  description: Option[String] = None,
  extraValues: Seq[String] = Nil,
  @since
  hidden: Boolean = false
) {

  /** [[value]], followed by [[extraValues]] */
  def values: Seq[String] = value +: extraValues

  /** Keeps this item only if some of its values start with `prefix`
    *
    * @return
    *   this item if `prefix` is empty, `None` if none of its values start with `prefix`, else an
    *   item with the same values (the first one being [[value]])
    */
  def withPrefix(prefix: String): Option[CompletionItem] =
    if (prefix.isEmpty) Some(this)
    else {
      val updatedValues = values.filter(_.startsWith(prefix))
      if (updatedValues.isEmpty) None
      else {
        val item = copy(value = values.head, extraValues = values.tail)
        Some(item)
      }
    }
}

object CompletionItem {

  /** Description of a hidden item, from the description of the thing it completes
    *
    * Starts with "(hidden)", like the descriptions of hidden things in help messages.
    */
  def hiddenDescription(description: Option[String]): Option[String] =
    Some("(hidden)" + description.filter(_.nonEmpty).fold("")(" " + _))

  /** Leaves hidden items out of `items`, if `items` also has non-hidden ones
    *
    * Hidden things, like options marked with [[caseapp.Hidden]] or hidden commands, are left out of
    * help messages, and of completions too when other things can be completed. When only hidden
    * things can be completed, they're kept, so that they can be completed when users type the
    * beginning of their names. The descriptions of hidden items say they're hidden (see
    * [[hiddenDescription]]).
    */
  def dropHidden(items: List[CompletionItem]): List[CompletionItem] =
    if (items.exists(!_.hidden)) items.filter(!_.hidden)
    else items
}

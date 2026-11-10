package caseapp.core.complete

import dataclass.data

/** A shell completion candidate
  *
  * @param value
  *   value to complete, like `--foo`
  * @param description
  *   description of the value, shown by shells that support it (zsh and fish)
  * @param extraValues
  *   other values with the same description, like aliases of an option (`-f`)
  */
@data(
  deprecatedSetters = true,
  deprecatedSettersMessage = "Use copy instead",
  deprecatedSettersSince = "2.1.2"
) case class CompletionItem(
  value: String,
  description: Option[String] = None,
  extraValues: Seq[String] = Nil
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
        val item = CompletionItem(values.head, description, values.tail)
        Some(item)
      }
    }
}

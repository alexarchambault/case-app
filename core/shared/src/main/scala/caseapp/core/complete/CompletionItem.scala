package caseapp.core.complete

import dataclass.data

@data class CompletionItem(
  value: String,
  description: Option[String] = None,
  extraValues: Seq[String] = Nil
) {
  def values: Seq[String] = value +: extraValues

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

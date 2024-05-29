package caseapp.core.complete

object Fish {

  val shellName: String =
    "fish"
  val id: String =
    s"$shellName-v1"

  def script(progName: String): String =
    s"""
    complete $progName -a '($progName complete $id (math 1 + (count (__fish_print_cmd_args))) (__fish_print_cmd_args))'
       |""".stripMargin

  private def escape(s: String): String =
    s.replace("\t", "  ").linesIterator.to(LazyList).headOption.getOrElse("")
  def print(items: Seq[CompletionItem]): String = {
    val newLine = System.lineSeparator()
    val b       = new StringBuilder
    for (item <- items; value <- item.values) {
      b.append(escape(value))
      for (desc <- item.description) {
        b.append("\t")
        b.append(escape(desc))
      }
      b.append(newLine)
    }
    b.result()
  }
}

package caseapp.core.complete

object Fish {

  val shellName: String =
    "fish"
  val id: String =
    s"$shellName-v1"

  def script(progName: String): String =
    script(progName, List("complete"))

  /** Shell script enabling completions for `progName`
    *
    * @param completeCommand
    *   name of the command that `progName` uses to compute completions (see
    *   `CommandsEntryPoint.completeCommandName`)
    */
  def script(progName: String, completeCommand: Seq[String]): String = {
    val complete = completeCommand.mkString(" ")
    s"""complete $progName -a '($progName $complete $id (math 1 + (count (__fish_print_cmd_args))) (__fish_print_cmd_args))'
       |""".stripMargin
  }

  private def escape(s: String): String =
    s.replace("\t", "  ").linesIterator.find(_ => true).getOrElse("")
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

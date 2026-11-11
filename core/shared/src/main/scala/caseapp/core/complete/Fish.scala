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
    // __fish_print_cmd_args prints the arguments before the current one, which is passed
    // on its own. Empty lines are dropped, as there's one when there are no completions.
    // -f disables the default file completion, that would mix file names with
    // the completions of the application: files are completed only when it has none.
    s"""function __${progName}_complete
       |    set -l previous (__fish_print_cmd_args)
       |    set -l current (commandline -ct)
       |    set -l completions ($progName $complete $id (math 1 + (count $$previous)) $$previous $$current | string match -v -- '')
       |    if set -q completions[1]
       |        printf '%s\\n' $$completions
       |    else
       |        __fish_complete_path $$current
       |    end
       |end
       |
       |complete -c $progName -f -a '(__${progName}_complete)'
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

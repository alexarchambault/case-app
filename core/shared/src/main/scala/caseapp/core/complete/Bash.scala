package caseapp.core.complete

object Bash {

  val shellName: String =
    "bash"
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
    val ifs      = "\\n"
    val complete = completeCommand.mkString(" ")
    s"""_${progName}_completions() {
       |  local IFS=$$'$ifs'
       |  eval "$$($progName $complete $id "$$(( $$COMP_CWORD + 1 ))" "$${COMP_WORDS[@]}")"
       |}
       |
       |complete -F _${progName}_completions $progName
       |""".stripMargin
  }

  // The output of print is evaluated by the function of script, so anything coming from
  // completion items is single-quoted, to be taken literally by bash
  private def quote(s: String): String =
    "'" + s.replace("'", "'\\''") + "'"
  private def firstLine(s: String): String =
    s.linesIterator.take(1).toList.headOption.getOrElse("")

  // When nothing can be suggested, complete file names, like bash does by default.
  // compopt -o filenames gets directory names a trailing '/' rather than a space.
  // As bash splits words on the characters of COMP_WORDBREAKS, like '=' or ':', the
  // current word can be such a character: complete files right after it then.
  private def fileCompletion: String =
    """compopt -o filenames 2>/dev/null; COMPREPLY=($(cur="${COMP_WORDS[$COMP_CWORD]}"; """ +
      """[[ ${#cur} == 1 && "$COMP_WORDBREAKS" == *"$cur"* ]] && cur=""; """ +
      """compgen -f -- "$cur"))"""

  def print(items: Seq[CompletionItem]): String = {
    val newLine     = System.lineSeparator()
    val b           = new StringBuilder
    val singleValue = items.iterator.flatMap(_.values).drop(1).isEmpty
    for (item <- items; value <- item.values) {
      val desc =
        if (singleValue) ""
        else item.description.map(firstLine).filter(_.nonEmpty).fold("")("  -- " + _)
      b.append(quote(firstLine(value) + desc))
      b.append(newLine)
    }
    if (b.isEmpty) fileCompletion
    else "COMPREPLY=(" + b.result() + ")"
  }
}

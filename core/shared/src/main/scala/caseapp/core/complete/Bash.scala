package caseapp.core.complete

object Bash {

  val shellName: String =
    "bash"
  val id: String =
    s"$shellName-v1"

  def script(progName: String): String = {
    val ifs = "\\n"
    s"""_${progName}_completions() {
       |  local IFS=$$'$ifs'
       |  eval "$$($progName complete $id "$$(( $$COMP_CWORD + 1 ))" "$${COMP_WORDS[@]}")"
       |}
       |
       |complete -F _${progName}_completions $progName
       |""".stripMargin
  }

  private def escape(s: String): String =
    s.replace("\"", "\\\"")
  def print(items: Seq[CompletionItem]): String = {
    val newLine     = System.lineSeparator()
    val b           = new StringBuilder
    val singleValue = items.iterator.flatMap(_.values).drop(1).isEmpty
    for (item <- items; value <- item.values) {
      b.append("\"")
      b.append(escape(value))
      for (desc <- item.description if !singleValue) {
        b.append("  -- ")
        b.append(escape(desc))
      }
      b.append("\"")
      b.append(newLine)
    }
    if (b.isEmpty) """COMPREPLY=($(compgen -f "${COMP_WORDS[$COMP_CWORD]}"))"""
    else "COMPREPLY=(" + b.result() + ")"
  }
}

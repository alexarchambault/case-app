package caseapp.core.complete

import scala.util.hashing.MurmurHash3

object Zsh {

  val shellName: String =
    "zsh"
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
    s"""#compdef _$progName $progName
       |
       |function _$progName {
       |  eval "$$($progName $complete $id $$CURRENT "$${words[@]}")"
       |}
       |""".stripMargin
  }

  private def hash(content: Iterator[String]): String = {
    val hash = MurmurHash3.arrayHash(content.toArray)
    if (hash < 0) (hash * -1).toString
    else hash.toString
  }

  // The output of print is evaluated by the function of script, so anything coming from
  // completion items is single-quoted, to be taken literally by zsh
  private def quote(s: String): String =
    "'" + s.replace("'", "'\\''") + "'"
  private def firstLine(s: String): String =
    s.linesIterator.take(1).toList.headOption.getOrElse("")

  // _describe splits its entries on the first unescaped ':', and unescapes what precedes it
  private def describeEscape(s: String): String =
    s.replace("\\", "\\\\").replace(":", "\\:")

  private def defs(item: CompletionItem): Seq[String] = {
    val desc = item.description.map(firstLine).filter(_.nonEmpty).fold("")(":" + _)
    item.values.map(value => quote(describeEscape(firstLine(value)) + desc))
  }

  // When nothing can be suggested, complete file names, also as '--option=…' values
  private def fileCompletion: String =
    "[[ $PREFIX == -*=* ]] && compset -P 1 '*='" + System.lineSeparator() +
      "_files" + System.lineSeparator()

  private def render(commands: Seq[String]): String =
    if (commands.isEmpty) fileCompletion
    else {
      val id = hash(commands.iterator)
      s"""local -a args$id
         |args$id=(
         |${commands.mkString(System.lineSeparator())}
         |)
         |_describe command args$id
         |""".stripMargin
    }
  def print(items: Seq[CompletionItem]): String =
    render(items.flatMap(defs(_)))
}

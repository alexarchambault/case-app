package caseapp.core.complete

import scala.util.hashing.MurmurHash3

object Zsh {

  val shellName: String =
    "zsh"
  val id: String =
    s"$shellName-v1"

  def script(progName: String): String =
    s"""#compdef _$progName $progName
       |typeset -A opt_args
       |
       |function _$progName {
       |  eval "$$($progName complete $id $$CURRENT $$words[@])"
       |}
       |""".stripMargin

  private def hash(content: Iterator[String]): String = {
    val hash = MurmurHash3.arrayHash(content.toArray)
    if (hash < 0) (hash * -1).toString
    else hash.toString
  }
  private def escape(s: String): String =
    s.replace("'", "\\'").replace("`", "\\`").linesIterator.toStream.headOption.getOrElse("")
  private def defs(item: CompletionItem): Seq[String] = {
    val (options, arguments) = item.values.partition(_.startsWith("-"))
    val optionsOutput =
      if (options.isEmpty) Nil
      else {
        val escapedOptions = options
        val desc           = item.description.map(desc => ":" + escape(desc)).getOrElse("")
        options.map { opt =>
          "\"" + opt + desc + "\""
        }
      }
    val argumentsOutput =
      if (arguments.isEmpty) Nil
      else {
        val desc = item.description.map(desc => ":" + escape(desc)).getOrElse("")
        arguments.map("'" + _.replace(":", "\\:") + desc + "'")
      }
    optionsOutput ++ argumentsOutput
  }

  private def render(commands: Seq[String]): String =
    if (commands.isEmpty) "_files" + System.lineSeparator()
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

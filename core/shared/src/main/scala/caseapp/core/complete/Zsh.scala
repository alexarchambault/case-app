package caseapp.core.complete

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

import dataclass.data

import scala.collection.mutable

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

  private def md5(content: Iterator[String]): String = {
    val md = MessageDigest.getInstance("MD5")
    for (s <- content) md.update(s.getBytes(StandardCharsets.UTF_8))
    val digest = md.digest()
    val res    = new BigInteger(1, digest).toString(16)
    if (res.length < 32)
      ("0" * (32 - res.length)) + res
    else
      res
  }

  private def defs(item: CompletionItem): Seq[String] = {
    val (options, arguments) = item.values.partition(_.startsWith("-"))
    val optionsOutput =
      if (options.isEmpty) Nil
      else {
        val escapedOptions = options
        val desc           = item.description.map(":" + _.replace("'", "\\'")).getOrElse("")
        options.map { opt =>
          "\"" + opt + desc + "\""
        }
      }
    val argumentsOutput =
      if (arguments.isEmpty) Nil
      else {
        val desc = item.description.map(":" + _.replace("'", "\\'")).getOrElse("")
        arguments.map("'" + _.replace(":", "\\:") + desc + "'")
      }
    optionsOutput ++ argumentsOutput
  }

  private def render(commands: Seq[String]): String =
    if (commands.isEmpty) "_files" + System.lineSeparator()
    else {
      val id = md5(commands.iterator)
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

package caseapp.docs

import caseapp.core.util.fansi

/** Helpers for the website pages, compiled by mdoc */
object DocHelpers {

  /** Prints a help message in a code block
    *
    * Meant to be called from `mdoc:passthrough` blocks. ANSI escape codes (colors…) are removed
    * from the help message.
    */
  def printHelp(message: String): Unit = {
    println("```text")
    println(new String(fansi.Str(message).getChars))
    println("```")
  }
}

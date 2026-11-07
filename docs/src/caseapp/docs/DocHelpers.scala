package caseapp.docs

import caseapp.core.util.fansi

/** Helpers for the website pages, compiled by mdoc */
object DocHelpers {

  /** Prints a help message in a code block
    *
    * Meant to be called from `mdoc:passthrough` blocks. The code block is written as HTML, so that
    * the colors and styles of the help message (ANSI escape codes) can be kept.
    */
  def printHelp(message: String): Unit =
    println(helpHtml(message))

  /** Converts a help message, that can contain ANSI escape codes, to an HTML code block */
  def helpHtml(message: String): String = {
    val str    = fansi.Str(message)
    val chars  = str.getChars
    val states = str.getColors
    val b      = new StringBuilder
    // Material for MkDocs styles these like the code blocks it generates
    b.append("<pre><code>")
    var start = 0
    while (start < chars.length) {
      val state = states(start)
      var end   = start + 1
      while (end < chars.length && states(end) == state)
        end += 1
      val text = escape(new String(chars, start, end - start))
      val css  = style(state)
      if (css.isEmpty) b.append(text)
      else b.append("<span style=\"").append(css).append("\">").append(text).append("</span>")
      start = end
    }
    b.append("</code></pre>")
    b.result()
  }

  private def escape(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

  // Terminal colors are rendered with the code highlighting colors of Material for MkDocs,
  // that are readable in both its light and dark modes (unlike, say, yellow on a light
  // background). Black, white, and background colors are ignored.
  private val colorVars: Map[fansi.Attr, String] = {
    import fansi.Color._
    Seq(
      Seq(Red, LightRed)                          -> "number",
      Seq(Green, LightGreen)                      -> "string",
      Seq(Yellow, LightYellow, Blue, LightBlue)   -> "keyword",
      Seq(Magenta, LightMagenta, Cyan, LightCyan) -> "function",
      Seq(DarkGray, LightGray)                    -> "comment"
    ).flatMap {
      case (colors, name) =>
        colors.map(_ -> s"var(--md-code-hl-$name-color)")
    }.toMap
  }

  private def style(state: fansi.Str.State): String = {
    val bold = fansi.Bold.lookupAttr(state & fansi.Bold.mask) == fansi.Bold.On
    val underlined =
      fansi.Underlined.lookupAttr(state & fansi.Underlined.mask) == fansi.Underlined.On
    val colorOpt = colorVars.get(fansi.Color.lookupAttr(state & fansi.Color.mask))
    Seq(
      if (bold) Some("font-weight: bold") else None,
      if (underlined) Some("text-decoration: underline") else None,
      colorOpt.map(c => s"color: $c")
    ).flatten.mkString("; ")
  }
}

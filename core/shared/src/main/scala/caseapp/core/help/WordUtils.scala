package caseapp.core.help

import java.util.regex.Pattern

object WordUtils {

  // adapted from https://github.com/apache/commons-lang/blob/601e976b0d5a9bb323fd2625c8d3751d1547a5d2/src/main/java/org/apache/commons/lang3/text/WordUtils.java#L273-L346
  def wrap(str: String, wrapLength: Int, newLineStrOpt: Option[String], wrapLongWords: Boolean, wrapOn: String): String = {

    val newLineStr = newLineStrOpt.getOrElse(System.lineSeparator())

    val wrapLength0 =
      if (wrapLength < 1) 1
      else wrapLength

    val wrapOn0 =
      if (isBlank(wrapOn)) " "
      else wrapOn

    val patternToWrapOn = Pattern.compile(wrapOn0)
    val inputLineLength = str.length
    var offset = 0
    val wrappedLine = new StringBuilder(inputLineLength + 32)

    var shouldStop = false

    while (!shouldStop && offset < inputLineLength) {
      var spaceToWrapAt = -1
      var matcher = patternToWrapOn.matcher(
        str.substring(offset, Math.min(Math.min(Integer.MAX_VALUE.toLong, offset + wrapLength0 + 1L).toInt, inputLineLength))
      )
      val found = matcher.find()
      if (found && matcher.start() == 0) {
        offset += matcher.end()
      } else {
        if (found)
          spaceToWrapAt = matcher.start() + offset

        // only last line without leading spaces is left
        if (inputLineLength - offset <= wrapLength0)
          shouldStop = true
        else {
          while (matcher.find())
            spaceToWrapAt = matcher.start() + offset

          if (spaceToWrapAt >= offset) {
            // normal case
            wrappedLine.append(str, offset, spaceToWrapAt)
            wrappedLine.append(newLineStr)
            offset = spaceToWrapAt + 1
          } else // really long word or URL
          if (wrapLongWords) {
            // wrap really long word one line at a time
            wrappedLine.append(str, offset, wrapLength0 + offset)
            wrappedLine.append(newLineStr)
            offset += wrapLength0
          } else {
            // do not wrap really long word, just extend beyond limit
            matcher = patternToWrapOn.matcher(str.substring(offset + wrapLength0))
            if (matcher.find())
              spaceToWrapAt = matcher.start() + offset + wrapLength0

            if (spaceToWrapAt >= 0) {
              wrappedLine.append(str, offset, spaceToWrapAt)
              wrappedLine.append(newLineStr)
              offset = spaceToWrapAt + 1
            } else {
              wrappedLine.append(str, offset, str.length())
              offset = inputLineLength
            }
          }
        }
      }
    }

    // Whatever is left in line is short enough to just pass through
    wrappedLine.append(str, offset, str.length())

    wrappedLine.toString
  }

  // adapted from https://github.com/apache/commons-lang/blob/601e976b0d5a9bb323fd2625c8d3751d1547a5d2/src/main/java/org/apache/commons/lang3/StringUtils.java#L3573-L3584
  private def isBlank(cs: CharSequence): Boolean =
    cs.length == 0 ||
      (0 until cs.length).forall(i => Character.isWhitespace(cs.charAt(i)))
}

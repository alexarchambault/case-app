package caseapp.core.help

import caseapp.core.util.fansi
import dataclass.data

@data class Table(lines: IndexedSeq[Seq[fansi.Str]]) {

  def widths: Seq[Int] =
    if (lines.isEmpty) Nil
    else
      lines.head.indices.map { i =>
        lines.iterator.map(_(i).length).max
      }

  def render(colSeparator: String, linePrefix: String, lineSeparator: String, defaultWidths: IndexedSeq[Int]): String = {
    val b = new StringBuilder
    render(b, colSeparator, linePrefix, lineSeparator, defaultWidths)
    b.result()
  }

  def render(b: StringBuilder, colSeparator: String, linePrefix: String, lineSeparator: String, defaultWidths: IndexedSeq[Int]): Unit =
    for ((line, lineIdx) <- lines.zipWithIndex) {
      b.append(linePrefix)
      val trailingEmptyCount = line.reverseIterator.takeWhile(_.length == 0).length
      for ((cell, colIdx) <- line.iterator.zipWithIndex) {
        val colDefaultWidth = defaultWidths(colIdx)
        b.append(cell.render)
        if (colIdx < line.length - 1 - trailingEmptyCount) {
          if (cell.length < colDefaultWidth)
            b.appendAll((0 until (colDefaultWidth - cell.length)).iterator.map(_ => ' '))
          b.append(colSeparator)
        }
      }
      if (lineIdx < lines.length - 1)
        b.append(lineSeparator)
    }

}

package caseapp.core.app

// from https://github.com/VirtusLab/scala-cli/blob/eced0b35c769eca58ae6f1b1a3be0f29a8700859/modules/cli/src/main/scala/scala/cli/internal/ProfileFileUpdater.scala

import caseapp.core.app.nio._

// initially adapted from https://github.com/coursier/coursier/blob/d9a0fcc1af4876bec7f19a18f2c93d808e06df8d/modules/env/src/main/scala/coursier/env/ProfileUpdater.scala#L44-L137

object ProfileFileUpdater {

  private def startEndIndices(start: String, end: String, content: String): Option[(Int, Int)] = {
    val startIdx = content.indexOf(start)
    if (startIdx >= 0) {
      val endIdx = content.indexOf(end, startIdx + 1)
      if (endIdx >= 0)
        Some(startIdx, endIdx + end.length)
      else
        None
    }
    else
      None
  }

  def addToProfileFile(
    file: Path,
    title: String,
    addition: String
  ): Boolean = {

    def updated(content: String): Option[String] = {
      val start    = s"# >>> $title >>>\n"
      val endStr   = s"# <<< $title <<<\n"
      val withTags = "\n" +
        start +
        addition.stripSuffix("\n") + "\n" + endStr
      if (content.contains(withTags))
        None
      else
        Some {
          startEndIndices(start, endStr, content) match {
            case None =>
              content + withTags
            case Some((startIdx, endIdx)) =>
              content.take(startIdx) +
                withTags +
                content.drop(endIdx)
          }
        }
    }

    var updatedSomething = false
    val contentOpt       = Some(file)
      .filter(FileOps.exists(_))
      .map(f => FileOps.readFile(f))
    for (updatedContent <- updated(contentOpt.getOrElse(""))) {
      Option(file.getParent).map(FileOps.createDirectories(_))
      FileOps.writeFile(file, updatedContent)
      updatedSomething = true
    }
    updatedSomething
  }

  def removeFromProfileFile(
    file: Path,
    title: String
  ): Boolean = {

    def updated(content: String): Option[String] = {
      val start = s"# >>> $title >>>\n"
      val end   = s"# <<< $title <<<\n"
      startEndIndices(start, end, content).map {
        case (startIdx, endIdx) =>
          content.take(startIdx).stripSuffix("\n") +
            content.drop(endIdx)
      }
    }

    var updatedSomething = false
    val contentOpt       = Some(file)
      .filter(FileOps.exists(_))
      .map(f => FileOps.readFile(f))
    for (updatedContent <- updated(contentOpt.getOrElse(""))) {
      Option(file.getParent).map(FileOps.createDirectories(_))
      FileOps.writeFile(file, updatedContent)
      updatedSomething = true
    }
    updatedSomething
  }
}

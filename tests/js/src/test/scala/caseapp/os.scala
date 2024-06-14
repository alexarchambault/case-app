package caseapp

import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global => g}

import java.util.regex.Pattern

object os {

  trait PathChunk {
    def segments: Seq[String]
    def ups: Int
  }

  object PathChunk {
    def checkSegment(s: String): Unit =
      () // TODO
    implicit class StringPathChunk(s: String) extends PathChunk {
      checkSegment(s)
      def segments            = Seq(s)
      def ups                 = 0
      override def toString() = s
    }
  }

  class RelPath private[os] (val ups: Int, val segments: Seq[String]) {
    def asSubPath: SubPath = {
      require(ups == 0)
      new SubPath(segments)
    }

    override def toString(): String =
      (Iterator.fill(ups)("..") ++ segments.iterator).mkString("/")
    override def hashCode = segments.hashCode() + ups.hashCode()
    override def equals(o: Any): Boolean = o match {
      case p: RelPath => segments == p.segments && p.ups == ups
      case p: SubPath => segments == p.segments && ups == 0
      case _          => false
    }
  }

  class SubPath private[os] (val segments: Seq[String]) {
    def /(chunk: PathChunk): SubPath = {
      require(chunk.ups <= segments.length)
      new SubPath(segments.take(segments.length - chunk.ups) ++ chunk.segments)
    }

    override def toString(): String =
      segments.mkString("/")
    override def hashCode = segments.hashCode()
    override def equals(o: Any): Boolean = o match {
      case p: SubPath => segments == p.segments
      case p: RelPath => segments == p.segments && p.ups == 0
      case _          => false
    }
  }

  object SubPath {
    val sub: SubPath = new SubPath(Seq.empty)
  }

  class Path private[os] (val underlying: String) {
    def /(chunk: PathChunk): Path = {
      val elems = List.fill(chunk.ups)("..") ++ chunk.segments
      val newPath = nodePath
        .applyDynamic("join")((underlying +: elems).map(x => x: js.Any): _*)
        .asInstanceOf[String]
      new Path(newPath)
    }

    def relativeTo(other: Path): os.RelPath = {
      val rel   = nodePath.relative(other.underlying, underlying).asInstanceOf[String]
      val elems = rel.split(Pattern.quote(nodePath.sep.asInstanceOf[String])).toSeq
      val ups   = elems.takeWhile(_ == "..").length
      new RelPath(ups, elems.drop(ups))
    }

    def toNIO                       = caseapp.core.app.nio.Path(underlying)
    override def toString(): String = underlying
  }

  object Path {
    def apply(path: String): Path =
      new Path(path)
  }

  def sub: SubPath = SubPath.sub

  private lazy val fs       = g.require("fs")
  private lazy val nodePath = g.require("path")
  private lazy val nodeOs   = g.require("os")

  object exists {
    def apply(path: Path): Boolean =
      fs.existsSync(path.underlying).asInstanceOf[Boolean]
  }

  object isDir {
    def apply(path: Path): Boolean =
      exists(path) &&
      fs.statSync(path.underlying).isDirectory().asInstanceOf[Boolean]
  }

  object read {
    def apply(path: Path): String =
      fs.readFileSync(path.underlying, js.Dictionary("encoding" -> "utf8"))
        .asInstanceOf[String]
  }

  object remove {
    object all {
      def apply(path: Path): Unit =
        fs.rmSync(path.underlying, js.Dictionary("recursive" -> true, "force" -> true))
    }
  }

  object temp {

    object dir {
      def apply(prefix: String): Path =
        new Path(fs.mkdtempSync(nodePath.join(nodeOs.tmpdir(), prefix)).asInstanceOf[String])
    }

  }

  object walk {
    def apply(path: Path): Seq[Path] =
      fs.readdirSync(path.underlying, js.Dictionary("recursive" -> true))
        .asInstanceOf[js.Array[String]]
        .toVector
        .map(subPath => Path(nodePath.join(path.underlying, subPath).asInstanceOf[String]))
  }

}

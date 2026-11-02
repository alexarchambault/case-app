package caseapp

import caseapp.demo.DerivedOptions
import utest.*

import scala.compiletime.testing.typeCheckErrors

object Scala3DerivationTests extends TestSuite {

  @AppName("Local app")
  final case class LocalDerivedOptions(foo: Int = 1) derives Parser, Help

  final class Holder {
    final case class NestedDerivedOptions(foo: Int = 1) derives Parser, Help
  }
  val holder = new Holder

  // one derivation used to expand to code nested too deeply for the compiler, past 80 fields or so
  // format: off
  final case class LargeOptions(
    f001: Int = 1, f002: Int = 2, f003: Int = 3, f004: Int = 4, f005: Int = 5, f006: Int = 6,
    f007: Int = 7, f008: Int = 8, f009: Int = 9, f010: Int = 10, f011: Int = 11, f012: Int = 12,
    f013: Int = 13, f014: Int = 14, f015: Int = 15, f016: Int = 16, f017: Int = 17, f018: Int = 18,
    f019: Int = 19, f020: Int = 20, f021: Int = 21, f022: Int = 22, f023: Int = 23, f024: Int = 24,
    f025: Int = 25, f026: Int = 26, f027: Int = 27, f028: Int = 28, f029: Int = 29, f030: Int = 30,
    f031: Int = 31, f032: Int = 32, f033: Int = 33, f034: Int = 34, f035: Int = 35, f036: Int = 36,
    f037: Int = 37, f038: Int = 38, f039: Int = 39, f040: Int = 40, f041: Int = 41, f042: Int = 42,
    f043: Int = 43, f044: Int = 44, f045: Int = 45, f046: Int = 46, f047: Int = 47, f048: Int = 48,
    f049: Int = 49, f050: Int = 50, f051: Int = 51, f052: Int = 52, f053: Int = 53, f054: Int = 54,
    f055: Int = 55, f056: Int = 56, f057: Int = 57, f058: Int = 58, f059: Int = 59, f060: Int = 60,
    f061: Int = 61, f062: Int = 62, f063: Int = 63, f064: Int = 64, f065: Int = 65, f066: Int = 66,
    f067: Int = 67, f068: Int = 68, f069: Int = 69, f070: Int = 70, f071: Int = 71, f072: Int = 72,
    f073: Int = 73, f074: Int = 74, f075: Int = 75, f076: Int = 76, f077: Int = 77, f078: Int = 78,
    f079: Int = 79, f080: Int = 80, f081: Int = 81, f082: Int = 82, f083: Int = 83, f084: Int = 84,
    f085: Int = 85, f086: Int = 86, f087: Int = 87, f088: Int = 88, f089: Int = 89, f090: Int = 90,
    f091: Int = 91, f092: Int = 92, f093: Int = 93, f094: Int = 94, f095: Int = 95, f096: Int = 96,
    f097: Int = 97, f098: Int = 98, f099: Int = 99, f100: Int = 100
  )
  // format: on

  final class NoInstances
  final case class WithoutArgParser(value: NoInstances)
  final case class WithoutParser(@Recurse value: NoInstances)

  val tests = Tests {
    test("derives clause") {
      test {
        val res = Parser[LocalDerivedOptions].parse(Seq("--foo", "2"))
        assert(res == Right((LocalDerivedOptions(2), Nil)))
        assert(Help[LocalDerivedOptions].appName == "Local app")
      }
      test("class nested in a class") {
        val res = Parser[holder.NestedDerivedOptions].parse(Nil)
        assert(res == Right((holder.NestedDerivedOptions(1), Nil)))
      }
      test("separately compiled") {
        val res = Parser[DerivedOptions].parse(Seq("-n", "3", "--values", "a"))
        assert(res == Right((DerivedOptions(3, List("a")), Nil)))
        assert(Help[DerivedOptions].appName == "Derived app")
      }
    }

    test("large class") {
      val res = Parser[LargeOptions].parse(Seq("--f100", "2"))
      assert(res == Right((LargeOptions(f100 = 2), Nil)))
    }

    test("compilation errors") {
      test("missing ArgParser") {
        val messages = typeCheckErrors("Parser[WithoutArgParser]").map(_.message)
        assert(messages.exists(_.startsWith("Cannot parse field value of ")))
        assert(!messages.exists(_.contains("Exception")))
      }
      test("missing Parser") {
        val messages = typeCheckErrors("Parser[WithoutParser]").map(_.message)
        assert(messages.exists(_.startsWith("Cannot derive a parser for field value of ")))
        assert(!messages.exists(_.contains("Exception")))
      }
      test("not a case class") {
        val messages = typeCheckErrors("Help[NoInstances]").map(_.message)
        assert(messages.exists(_.startsWith("Cannot derive a help message for ")))
        assert(!messages.exists(_.contains("Exception")))
      }
    }
  }
}

package caseapp

import caseapp.demo.SeparatelyCompiledOptions
import utest._

object DerivationTests extends TestSuite {

  final case class Tagged(
    @ExtraName("a") @ExtraName("b") first: Int = 1,
    required: String,
    @Tag("z") @Tag("a") third: Int = 3
  )

  final case class ImplicitDefaults(
    list: List[Int],
    vector: Vector[String],
    option: Option[String],
    count: Int @@ Counter
  )

  final case class Options(foo: Int = 0)

  final class Holder {
    final case class Inner(first: Int = 1, second: String = "default")
  }
  val holder = new Holder

  val tests = Tests {
    test("use Default instances for fields with no default value") {
      val res      = Parser[ImplicitDefaults].parse(Nil)
      val expected = Right((ImplicitDefaults(Nil, Vector.empty, None, Tag.of(0)), Nil))
      assert(res == expected)
    }

    test("keep the order of annotations") {
      def check(parser: Parser[_]) = {
        val names = parser.args.map(_.extraNames.map(_.name))
        val tags  = parser.args.map(_.tags.map(_.name))
        assert(names == Seq(Seq("a", "b"), Nil, Nil))
        assert(tags == Seq(Nil, Nil, Seq("z", "a")))
      }
      test("same compilation run")(check(Parser[Tagged]))
      test("separate compilation run")(check(Parser[SeparatelyCompiledOptions]))
    }

    test("default values of separately compiled class") {
      val res      = Parser[SeparatelyCompiledOptions].parse(Seq("--required", "value"))
      val expected = Right((SeparatelyCompiledOptions(1, "value", 3), Nil))
      assert(res == expected)
    }

    test("default values of class nested in a class") {
      val res      = Parser[holder.Inner].parse(Nil)
      val expected = Right((holder.Inner(1, "default"), Nil))
      assert(res == expected)
    }

    test("keep the name of a class only named Options") {
      val help = Help[Options]
      assert(help.appName == "Options")
      assert(help.progName == "options")
    }
  }
}

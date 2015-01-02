package caseapp

import java.util.GregorianCalendar

import scala.util.Success
import org.scalatest._

object Tests {

  case class NoArgs()
  
  case class FewArgs(
    value  : String = "default"
  , numFoo : Int = -10
  )
  
  case class MoreArgs(
    count  : List[Unit]
  , few    : FewArgs = FewArgs()
  )

  case class WithList(
    list   : List[Int]
  )

  case class WithTaggedList(
    list   : List[String]
  )

  case class WithCalendar(
    date   : java.util.Calendar
  )

  case class Demo(
    first: Boolean = false
  , @ExtraName("V") value: Option[String] = None
  , @ExtraName("v") verbose: List[Unit]
  , @ExtraName("S") stages: List[String]
  ) extends App
  
  implicitly[Parser[WithHelp[Demo]]]
  
}

class Tests extends FlatSpec with Matchers {
  
  import Tests._

  "A parser" should "parse no args" in {
    val parser = implicitly[Parser[NoArgs]]
    Success((NoArgs(), Nil)) shouldEqual parser(Nil)
  }

  it should "give remaining args as is" in {
    val parser = implicitly[Parser[NoArgs]]
    parser(List("user arg", "other user arg")) shouldEqual Success((NoArgs(), List("user arg", "other user arg"))) 
  }

  it should "parse no args and return default values and remaining args" in {
    val parser = implicitly[Parser[FewArgs]]
    parser(List("user arg", "other user arg")) shouldEqual Success((FewArgs(), List("user arg", "other user arg")))
  }

  it should "parse a few args and return a default value and remaining args" in {
    val parser = implicitly[Parser[FewArgs]]
    parser(List("user arg", "--num-foo", "4", "other user arg")) shouldEqual Success((FewArgs(numFoo = 4), List("user arg", "other user arg")))
  }

  it should "parse a args recursively and return a default value and remaining args" in {
    val parser = implicitly[Parser[MoreArgs]]
    parser(List("user arg", "--num-foo", "4", "--count", "other user arg", "--count")) shouldEqual Success((MoreArgs(count = List.fill(2)(()), few = FewArgs(numFoo = 4)), List("user arg", "other user arg")))
  }
  
  it should "parse args" in {
    val parser = implicitly[Parser[demo.Demo]]
    parser(List("user arg", "--stages", "first", "--value", "Some value", "--verbose", "--verbose", "--verbose", "other user arg", "--stages", "second", "--first")) shouldEqual Success((demo.Demo(first = true, value = Some("Some value"), verbose = List.fill(3)(()), stages = List("first", "second")), List("user arg", "other user arg")))
  }

  it should "parse short args" in {
    val parser = implicitly[Parser[demo.Demo]]
    parser(List("user arg", "-S", "first", "--value", "Some value", "-v", "-v", "-v", "other user arg", "-S", "second", "--first")) shouldEqual Success((demo.Demo(first = true, value = Some("Some value"), verbose = List.fill(3)(()), stages = List("first", "second")), List("user arg", "other user arg")))
  }

  it should "parse list args" in {
    val parser = implicitly[Parser[WithList]]
    parser(List("--list", "2", "--list", "5", "extra")) shouldEqual Success((WithList(list = List(2, 5)), List("extra")))
  }

  it should "parse semi-colon separated list args" in {
    val parser = implicitly[Parser[WithTaggedList]]
    parser(List("--list", "foo", "--list", "bar", "--list", "other", "extra2")) shouldEqual Success((WithTaggedList(list = List("foo", "bar", "other")), List("extra2")))
  }

  it should "parse a date" in {
    val parser = implicitly[Parser[WithCalendar]]
    parser(List("--date", "2014-10-23")) shouldEqual Success((WithCalendar(date = {
      new GregorianCalendar(2014, 9, 23)
    }), Nil))
  }

}

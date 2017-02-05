package caseapp
package core

trait ArgParser[T] {
  def apply(current: Option[T], s: String, mandatory: Boolean): Either[String, (Boolean, T)]
  def apply(current: Option[T]): Either[String, T]
  def isFlag: Boolean = false
  def description: String
}

object ArgParser extends PlatformArgParsers {
  def apply[T](implicit parser: ArgParser[T]): ArgParser[T] = parser

  def instance[T](hintDescription: String)(f: String => Either[String, T]): ArgParser[T] =
    new ArgParser[T] {
      def apply(current: Option[T], s: String, mandatory: Boolean) = f(s).right.map((true, _))
      def apply(current: Option[T]) = Left("argument missing")
      def description = hintDescription
    }

  def flag[T](hintDescription: String)(f: Option[String] => Either[String, T]): ArgParser[T] =
    new ArgParser[T] {
      def apply(current: Option[T], s: String, mandatory: Boolean) =
        f(if (mandatory) Some(s) else None).right.map((mandatory, _))
      def apply(current: Option[T]) = f(None)
      override def isFlag = true
      def description = hintDescription
    }

  def accumulator[T](hintDescription: String)(f: (Option[T], String) => Either[String, T]): ArgParser[T] =
    new ArgParser[T] {
      def apply(current: Option[T], s: String, mandatory: Boolean) =
        f(current, s).right.map((true, _))
      def apply(current: Option[T]) = Left("argument missing")
      def description = hintDescription
    }

  def flagAccumulator[T](hintDescription: String)(f: (Option[T], Option[String]) => Either[String, T]): ArgParser[T] =
    new ArgParser[T] {
      def apply(current: Option[T], s: String, mandatory: Boolean) =
        f(current, if (mandatory) Some(s) else None).right.map((mandatory, _))
      def apply(current: Option[T]) = f(current, None)
      override def isFlag = true
      def description = hintDescription
    }

  implicit def int: ArgParser[Int] =
    instance("int") { s =>
      try Right(s.toInt)
      catch { case _: NumberFormatException =>
        Left(s"Malformed integer: $s")
      }
    }
  implicit def long: ArgParser[Long] =
    instance("long") { s =>
      try Right(s.toLong)
      catch { case _: NumberFormatException =>
        Left(s"Malformed long integer: $s")
      }
    }
  implicit def double: ArgParser[Double] =
    instance("double") { s =>
      try Right(s.toDouble)
      catch { case _: NumberFormatException =>
        Left(s"Malformed double float: $s")
      }
    }
  implicit def float: ArgParser[Float] =
    instance("float") { s =>
      try Right(s.toFloat)
      catch { case _: NumberFormatException =>
        Left(s"Malformed float: $s")
      }
    }
  implicit def bigDecimal: ArgParser[BigDecimal] =
    instance("decimal") { s =>
      try Right(BigDecimal(s))
      catch { case _: NumberFormatException =>
        Left(s"Malformed decimal: $s")
      }
    }
  implicit def string: ArgParser[String] =
    instance("string")(Right(_))
  implicit def unit: ArgParser[Unit] = {
    val trues = Set("true", "1")
    val falses = Set("false", "0")

    flag("flag/unit") {
      case None => Right(())
      case Some(s) =>
        if (trues(s))
          Right(())
        else if (falses(s))
          Left(s"Option cannot be explicitly disabled")
        else
          Left(s"Unrecognized flag value: $s")
    }
  }
  implicit def boolean: ArgParser[Boolean] = {
    val trues = Set("true", "1")
    val falses = Set("false", "0")

    flag("bool") {
      case None => Right(true)
      case Some(s) =>
        if (trues(s))
          Right(true)
        else if (falses(s))
          Right(false)
        else
          Left(s"Unrecognized flag value: $s")
    }
  }
  implicit def counter: ArgParser[Int @@ Counter] =
    flagAccumulator("counter") { (prevOpt, s) =>
      Right(Tag.of(prevOpt.fold(0)(Tag.unwrap) + 1))
    }

  // FIXME list and option below may not be fine with lists/options of flags
  implicit def list[T: ArgParser]: ArgParser[List[T]] = {
    val parser = ArgParser[T]
    accumulator(s"${parser.description}*") { (prevOpt, s) =>
      parser.apply(None, s, mandatory = true).right.flatMap {
        case (false, _) =>
          // should not happen
          Left(s"Unrecognized value: $s")
        case (true, t) =>
          // inefficient for big lists
          Right(prevOpt.getOrElse(Nil) :+ t)
      }
    }
  }
  implicit def option[T: ArgParser]: ArgParser[Option[T]] = {
    val parser = ArgParser[T]
    accumulator(s"${parser.description}?") { (prevOpt, s) =>
      parser.apply(prevOpt.flatten, s, mandatory = true).right.flatMap {
        case (false, _) =>
          // should not happen
          Left(s"Unrecognized value: $s")
        case (true, t) =>
          Right(Some(t))
      }
    }
  }
}


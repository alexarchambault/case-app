package caseapp
package core

import java.text.SimpleDateFormat
import java.util.GregorianCalendar

import shapeless.labelled.{ FieldType, field }
import shapeless.{ :: => _, _ }
import scala.util.{ Try, Success, Failure }


private[core] case class ArgParserException(withName: String => Exception) extends Exception("[unknown]")


/**
 * A type class which given names, returns a NamedArgParser
 */
trait ArgParser[T] {
  def apply(names: (Option[Names], List[Name])): NamedArgParser[T]
}

object ArgParser {
  def apply[T](implicit argParser: ArgParser[T]): ArgParser[T] = argParser

  def value[T](f: (T, String) => Try[T]): ArgParser[T] = _value[T](isFlag = false, {
    (current, args) => args match {
      case Nil =>
        Failure(ArgParserException(name => new Exception(s"Missing value of argument $name")))
      case head :: tail =>
        f(current, head).map(t => Some((t, tail)))
    }
  })

  def flag[T](f: (T, List[String]) => Try[Option[(T, List[String])]]): ArgParser[T] = _value[T](isFlag = true, f)
  
  private def _value[T](isFlag: Boolean, f: (T, List[String]) => Try[Option[(T, List[String])]]): ArgParser[T] = ArgParser.from { _names =>
    val names = _names._2

    NamedArgParser.from(NamesInfo(names.map(_.option), isFlag) :: Nil) { (current, args) =>
      (Option.empty[List[String]] /: names) {
        case (None, name) => name(args, isFlag)
        case (acc, _)     => acc
      } match {
        case Some(remainingArgs) =>
          f(current, remainingArgs) match {
            case Failure(t: ArgParserException) => Failure(t.withName(names.head.name))
            case t => t
          }
        case None =>
          Success(None)
      }
    }
  }

  import shapeless.::

  implicit val hnilArgParser: ArgParser[HNil] = ArgParser.from { names =>
    NamedArgParser.from(Nil) { (_, _) =>
      Success(None)
    }
  }

  implicit def hconsArgParser[K <: Symbol, H, T <: HList] (implicit 
    key: Witness.Aux[K]
  , headArgParser: Lazy[ArgParser[H]]
  , tailArgParser: Lazy[ArgParser[T]]
  ): ArgParser[FieldType[K, H] :: T] =
    ArgParser.from { names =>
      def default = List(Name(util.pascalCaseSplit(key.value.name.toList).map(_.toLowerCase).mkString("-")))
      val headUnderlying = headArgParser.value({
        // A bit hack-ish, for custom argument names whose types are also case classes.
        // Using a proper type class for NamesOf should solve that.
        val e = names._1.getOrElse(Names(Nil)).names.find(_._1 == key.value.name).map(_._2)
        (e.flatMap(_.left.map(Some(_)).left getOrElse None), e.flatMap(_.right.toOption).getOrElse(Nil) ++ default)
      })
      val tailUnderlying = tailArgParser.value(names)

      NamedArgParser.from(headUnderlying.namesInfos ::: tailUnderlying.namesInfos) { (l, args) =>
        headUnderlying(l.head, args) match {
          case Success(Some((h, args))) =>
            Success(Some((field[K](h) :: l.tail, args)))
          case Success(None) =>
            tailUnderlying(l.tail, args) match {
              case Success(Some((t, args))) =>
                Success(Some((l.head :: t, args)))
              case Success(None) =>
                Success(None)
              case Failure(t) =>
                Failure(t)
            }
          case Failure(t) =>
            Failure(t)
        }
      }
    }

  // Using a specific implicit for tagged types here, see https://github.com/milessabin/shapeless/issues/309
  implicit def hconsTaggedHeadArgParser[K <: Symbol, H, HT, T <: HList] (implicit
    key: Witness.Aux[K]
  , headArgParser: Lazy[ArgParser[H @@ HT]]
  , tailArgParser: Lazy[ArgParser[T]]
  ): ArgParser[FieldType[K, H @@ HT] :: T] = hconsArgParser[K, H @@ HT, T]

  implicit def instanceArgParser[F, G <: HList] (implicit
    gen: LabelledGeneric.Aux[F, G]
  , underlying: Lazy[ArgParser[G]]
  ): ArgParser[F] =
    ArgParser.from {
      val u = underlying.value

      names =>
        val _underlying = u(names)
        NamedArgParser.from(_underlying.namesInfos) { (f, args) =>
          _underlying(gen.to(f), args).map(_.map{ case (g, args) =>
            (gen.from(g), args)
          })
        }
    }


  def from[T](f: ((Option[Names], List[Name])) => NamedArgParser[T]): ArgParser[T] = new ArgParser[T] {
    def apply(names: (Option[Names], List[Name])) = f(names)
  }

  implicit val unitArgParser: ArgParser[Unit] = {
    ArgParser.flag { (current, args) =>
      Success(Some(((), args)))
    }
  }

  implicit val booleanArgParser: ArgParser[Boolean] = {
    ArgParser.flag { (current, args) =>
      Success(Some((true, args)))
    }
  }

  implicit val intArgParser: ArgParser[Int] = ArgParser.value { (current, head) =>
    Try(head.toInt)
  }

  implicit val intCounterArgParser: ArgParser[Int @@ Counter] = ArgParser.flag { (current, args) =>
    Try(Some((Tag.of(Tag.unwrap(current) + 1), args)))
  }

  implicit val longArgParser: ArgParser[Long] = ArgParser.value { (current, head) =>
    Try(head.toLong)
  }

  implicit val floatArgParser: ArgParser[Float] = ArgParser.value { (current, head) =>
    Try(head.toFloat)
  }

  implicit val doubleArgParser: ArgParser[Double] = ArgParser.value { (current, head) =>
    Try(head.toDouble)
  }

  implicit val stringArgParser: ArgParser[String] = ArgParser.value { (current, head) =>
    Try(head)
  }

  private val fmt = new SimpleDateFormat("yyyy-MM-dd")

  implicit val dateArgParser: ArgParser[java.util.Calendar] = ArgParser.value { (current, head) =>
    Try {
      val c = new GregorianCalendar
      c setTime fmt.parse(head)
      c
    }
  }

  implicit def optionArgParser[T: ArgParser : Default]: ArgParser[Option[T]] = ArgParser.from { names =>
    val underlying = ArgParser[T].apply(names)

    NamedArgParser.from(underlying.namesInfos) { (tOption, args) =>
      underlying(tOption getOrElse Default[T].apply(), args).map(_.map{case (t, args) =>
        (Some(t), args)
      })
    }
  }

  implicit def listArgParser[T: ArgParser : Default]: ArgParser[List[T]] = ArgParser.from[List[T]] { names =>
    val underlying = ArgParser[T].apply(names)

    NamedArgParser.from(underlying.namesInfos) { (current, head) =>
      underlying.apply(current.headOption getOrElse Default[T].apply(), head).map(_.map{case (t, l) => (current :+ t, l) })
    }
  }
}



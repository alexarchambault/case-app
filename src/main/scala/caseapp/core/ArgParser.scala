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
  def apply(names: Either[Names, List[Name]]): NamedArgParser[T]
}

object ArgParser {
  def apply[T](implicit argParser: ArgParser[T]): ArgParser[T] = argParser

  def singleValue[T](f: (T, String) => Try[Option[T]]): ArgParser[T] = _singleValue[T](isFlag = false, {
    (current, args) => args match {
      case Nil =>
        Failure(ArgParserException(name => new Exception(s"Missing value of argument $name")))
      case head :: tail =>
        f(current, head).map(_.map(t => (t, tail)))
    }
  })

  def flag[T](f: (T, List[String]) => Try[Option[(T, List[String])]]): ArgParser[T] = _singleValue[T](isFlag = true, f)
  
  private def _singleValue[T](isFlag: Boolean, f: (T, List[String]) => Try[Option[(T, List[String])]]): ArgParser[T] = ArgParser.from { _names =>
    val names = _names.right getOrElse Nil

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
      val headUnderlying = headArgParser.value(names.left.getOrElse(Names(Nil)).names.find(_._1 == key.value.name).map(_._2) getOrElse Right(Nil))
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


  def from[T](f: Either[Names, List[Name]] => NamedArgParser[T]): ArgParser[T] = new ArgParser[T] {
    def apply(names: Either[Names, List[Name]]) = f(names)
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

  implicit val intArgParser: ArgParser[Int] = ArgParser.singleValue { (current, head) =>
    Try(Some(head.toInt))
  }

  implicit val intCounterArgParser: ArgParser[Int @@ Counter] = ArgParser.flag { (current, args) =>
    Try(Some((Tag.of(Tag.unwrap(current) + 1), args)))
  }

  implicit val longArgParser: ArgParser[Long] = ArgParser.singleValue { (current, head) =>
    Try(Some(head.toLong))
  }

  implicit val floatArgParser: ArgParser[Float] = ArgParser.singleValue { (current, head) =>
    Try(Some(head.toFloat))
  }

  implicit val doubleArgParser: ArgParser[Double] = ArgParser.singleValue { (current, head) =>
    Try(Some(head.toDouble))
  }

  implicit val stringArgParser: ArgParser[String] = ArgParser.singleValue { (current, head) =>
    Try(Some(head))
  }

  private val fmt = new SimpleDateFormat("yyyy-MM-dd")

  implicit val dateArgParser: ArgParser[java.util.Calendar] = ArgParser.singleValue { (current, head) =>
    Try(Some{
      val c = new GregorianCalendar
      c.setTime(fmt parse head)
      c
    })
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



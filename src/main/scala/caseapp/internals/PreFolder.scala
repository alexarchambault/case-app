package caseapp
package internals

import java.text.SimpleDateFormat
import java.util.GregorianCalendar

import shapeless.labelled.{ FieldType, field }
import shapeless.{ :: => _, _ }
import scalaz.{ Tag, @@ }
import scala.util.{ Try, Success, Failure }


private[internals] case class PreFolderException(withName: String => Exception) extends Exception("[unknown]")


trait PreFolder[T] {
  def apply(names: Either[RecNames, List[Name]]): Folder[T]
}

object PreFolderSingleValue {
  def apply[T](f: (T, String) => Try[Option[T]]): PreFolder[T] = _apply[T](isFlag = false, {
    (current, args) => args match {
      case Nil =>
        Failure(PreFolderException(name => new Exception(s"Missing value of argument $name")))
      case head :: tail =>
        f(current, head).map(_.map(t => (t, tail)))
    }
  })

  def flag[T](f: (T, List[String]) => Try[Option[(T, List[String])]]): PreFolder[T] = _apply[T](isFlag = true, f)
  
  private def _apply[T](isFlag: Boolean, f: (T, List[String]) => Try[Option[(T, List[String])]]): PreFolder[T] = PreFolder.preFolder { _names =>
    val names = _names.right getOrElse Nil

    Folder(ArgDescription(names.map(_.option), if (isFlag) None else Some("value"), Nil) :: Nil) { (current, args) =>
      (Option.empty[List[String]] /: names) {
        case (None, name) => name(args, isFlag)
        case (acc, _)     => acc
      } match {
        case Some(remainingArgs) =>
          f(current, remainingArgs) match {
            case Failure(t: PreFolderException) => Failure(t.withName(names.head.name))
            case t => t
          }
        case None =>
          Success(None)
      }
    }
  }
}

object PreFolder {
  import shapeless.::

  implicit val hnilPreFolder: PreFolder[HNil] = PreFolder.preFolder { names =>
    Folder(Nil) { (_, _) =>
      Success(None)
    }
  }

  implicit def hconsPreFolder[K <: Symbol, H, T <: HList] (implicit 
    key: Witness.Aux[K]
  , headPreFolder: Lazy[PreFolder[H]]
  , tailPreFolder: Lazy[PreFolder[T]]
  ): PreFolder[FieldType[K, H] :: T] =
    PreFolder.preFolder { names =>
      val headUnderlying = headPreFolder.value(names.left.getOrElse(RecNames(Nil)).names.find(_._1 == key.value.name).map(_._2) getOrElse Right(Nil))
      val tailUnderlying = tailPreFolder.value(names)

      Folder(headUnderlying.descriptions ::: tailUnderlying.descriptions) { (l, args) =>
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

  implicit def preFolderProject[F, G <: HList] (implicit
    gen: LabelledGeneric.Aux[F, G]
  , underlying: Lazy[PreFolder[G]]
  ): PreFolder[F] = {
    val u = underlying.value
    
    PreFolder.preFolder {
      names =>
        val _underlying = u(names)
        Folder(_underlying.descriptions) { (f, args) =>
          _underlying(gen.to(f), args).map(_.map{ case (g, args) =>
            (gen.from(g), args)
          })
        }
    }
  }


  def preFolder[T](f: Either[RecNames, List[Name]] => Folder[T]): PreFolder[T] = new PreFolder[T] {
    def apply(names: Either[RecNames, List[Name]]) = f(names)
  }

  implicit val unitPreFolder: PreFolder[Unit] = {
    PreFolderSingleValue.flag { (current, args) =>
      Success(Some(((), args)))
    }
  }

  implicit val booleanPreFolder: PreFolder[Boolean] = {
    PreFolderSingleValue.flag { (current, args) =>
      Success(Some((true, args)))
    }
  }

  implicit val intPreFolder: PreFolder[Int] = PreFolderSingleValue { (current, head) =>
    Try(Some(head.toInt))
  }

  implicit val longPreFolder: PreFolder[Long] = PreFolderSingleValue { (current, head) =>
    Try(Some(head.toLong))
  }

  implicit val floatPreFolder: PreFolder[Float] = PreFolderSingleValue { (current, head) =>
    Try(Some(head.toFloat))
  }

  implicit val doublePreFolder: PreFolder[Double] = PreFolderSingleValue { (current, head) =>
    Try(Some(head.toDouble))
  }

  implicit val stringPreFolder: PreFolder[String] = PreFolderSingleValue { (current, head) =>
    Try(Some(head))
  }

  val fmt = new SimpleDateFormat("yyyy-MM-dd")

  implicit val datePreFolder: PreFolder[java.util.Calendar] = PreFolderSingleValue { (current, head) =>
    Try(Some{
      val c = new GregorianCalendar
      c.setTime(fmt parse head)
      c
    })
  }

  import language.implicitConversions
  private implicit def unwrap[A, T](a: A @@ T): A = Tag.unsubst[A, scalaz.Id.Id, T](a)

  implicit val intCounterPreFolder: PreFolder[Int @@ Counter] = PreFolderSingleValue.flag { (current, args) =>
    Try(Some((Tag.of(current + 1), args)))
  }

  implicit def optionPreFolder[T: PreFolder : Default]: PreFolder[Option[T]] = PreFolder.preFolder { names =>
    val underlying = implicitly[PreFolder[T]].apply(names)

    Folder(underlying.descriptions) { (tOption, args) =>
      underlying(tOption getOrElse implicitly[Default[T]].apply(), args).map(_.map{case (t, args) =>
        (Some(t), args)
      })
    }
  }

  implicit def listPreFolder[T: PreFolder : Default]: PreFolder[List[T]] = PreFolder.preFolder[List[T]] { names =>
    val underlying = implicitly[PreFolder[T]].apply(names)

    Folder(underlying.descriptions) { (current, head) =>
      underlying.apply(current.headOption getOrElse implicitly[Default[T]].apply(), head).map(_.map{case (t, l) => (current :+ t, l) })
    }
  }

}



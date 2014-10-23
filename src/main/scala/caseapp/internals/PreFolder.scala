package caseapp
package internals

import java.text.SimpleDateFormat
import java.util.GregorianCalendar

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

object PreFolderTC extends LabelledProductTypeClass[PreFolder] {
  import shapeless.::

  def product[H, T <: HList](name: String, cHead: PreFolder[H], cTail: PreFolder[T]): PreFolder[H :: T] =
    PreFolder.preFolder { names =>
      val headUnderlying = cHead(names.left.getOrElse(RecNames(Nil)).names.find(_._1 == name).map(_._2) getOrElse Right(Nil))
      val tailUnderlying = cTail(names)

      Folder(headUnderlying.descriptions ::: tailUnderlying.descriptions) { (l, args) =>
        headUnderlying(l.head, args) match {
          case Success(Some((h, args))) =>
            Success(Some((h :: l.tail, args)))
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

  val emptyProduct: PreFolder[HNil] = PreFolder.preFolder { names =>
    Folder(Nil) { (_, _) =>
      Success(None)
    }
  }

  def project[F, G](instance: => PreFolder[G], to: F => G, from: G => F): PreFolder[F] = PreFolder.preFolder {
    val _instance = instance

    names =>
      val underlying = _instance(names)
      Folder(underlying.descriptions) { (f, args) =>
        underlying(to(f), args).map(_.map{ case (g, args) =>
          (from(g), args)
        })
      }
  }

}

object PreFolder extends LabelledProductTypeClassCompanion[PreFolder] {
  implicit val tc: LabelledProductTypeClass[PreFolder] = PreFolderTC

  import language.experimental.macros

  // Already in PreFolder.auto, copied here for more convient use
  implicit def derive[T](implicit ev: LabelledProductTypeClass[PreFolder]): PreFolder[T] =
     macro shapeless.GenericMacros.deriveLabelledProductInstance[PreFolder, T]


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



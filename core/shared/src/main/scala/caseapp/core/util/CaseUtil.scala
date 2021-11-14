package caseapp.core.util

object CaseUtil {

  def pascalCaseSplit(s: List[Char]): List[String] =
    if (s.isEmpty)
      Nil
    else if (!s.head.isUpper) {
      val (w, tail) = s.span(!_.isUpper)
      w.mkString :: pascalCaseSplit(tail)
    }
    else if (s.tail.headOption.forall(!_.isUpper)) {
      val (w, tail) = s.tail.span(!_.isUpper)
      (s.head :: w).mkString :: pascalCaseSplit(tail)
    }
    else {
      val (w, tail) = s.span(_.isUpper)
      if (tail.isEmpty)
        w.mkString :: pascalCaseSplit(tail)
      else
        w.init.mkString :: pascalCaseSplit(w.last :: tail)
    }

}

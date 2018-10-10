package caseapp

import caseapp.core.Error
import caseapp.core.argparser.ArgParser
import eu.timepit.refined.api.{RefType, Refined, Validate}

package object refined {

  implicit def refinedArgParser[T, P](implicit
    argParser: ArgParser[T],
    validate: Validate[T, P]
  ): ArgParser[Refined[T, P]] =
    argParser.xmapError(
      _.value,
      t => {
        val res = validate.validate(t)
        if (res.isPassed) Right(RefType.refinedRefType.unsafeWrap(t))
        else Left(Error.Other(validate.showExpr(t)))
      }
    )

}

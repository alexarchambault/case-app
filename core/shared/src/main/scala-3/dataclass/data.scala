// same as https://github.com/alexarchambault/data-class/blob/cb28e25100090785fc6ae790a6cc9f6f79bb2043/src/main/scala/dataclass/data.scala
// but stripping the macros

package dataclass

import scala.annotation.{StaticAnnotation, compileTimeOnly}

@compileTimeOnly("enable macro paradise to expand macro annotations")
class data(
  apply: Boolean = true,
  publicConstructor: Boolean = true,
  optionSetters: Boolean = false,
  settersCallApply: Boolean = false
) extends StaticAnnotation

// from https://github.com/alexarchambault/data-class/blob/cb28e25100090785fc6ae790a6cc9f6f79bb2043/src/main/scala/dataclass/since.scala

package dataclass

import scala.annotation.StaticAnnotation

class since(val version: String = "") extends StaticAnnotation

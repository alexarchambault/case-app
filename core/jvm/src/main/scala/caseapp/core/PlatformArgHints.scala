package caseapp
package core

import java.util.Calendar

trait PlatformArgHints {
  implicit def calendar: ArgHint[Calendar] = new ArgHint[Calendar] {
    override def description = "yyyy-MM-dd"
    override def isRequired = true
  }
}

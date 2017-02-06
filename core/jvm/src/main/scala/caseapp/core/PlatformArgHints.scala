package caseapp
package core

import java.util.Calendar

trait PlatformArgHints {
  implicit def calendar: ArgHint[Calendar] = ArgHint.hint("yyyy-MM-dd")
}

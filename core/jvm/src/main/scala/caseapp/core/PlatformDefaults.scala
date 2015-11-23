package caseapp.core

import java.util.{ Calendar, GregorianCalendar }

trait PlatformDefaults {

  implicit val calendar: Default[Calendar] = Default.instance(new GregorianCalendar())

}

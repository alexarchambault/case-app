package caseapp

case class AppName(appName: String) extends annotation.StaticAnnotation {
  private[caseapp] def orElse(other: AppName): AppName = AppName(
    if (appName.nonEmpty) appName else other.appName
  )
}

case class ProgName(progName: String) extends annotation.StaticAnnotation {
  private[caseapp] def orElse(other: ProgName): ProgName = ProgName(
    if (progName.nonEmpty) progName else other.progName
  )
}

case class AppVersion(appVersion: String) extends annotation.StaticAnnotation {
  private[caseapp] def orElse(other: AppVersion): AppVersion = AppVersion(
    if (appVersion.nonEmpty) appVersion else other.appVersion
  )
}

case class ArgsName(argsName: String) extends annotation.StaticAnnotation {
  private[caseapp] def orElse(other: ArgsName): ArgsName = ArgsName(
    if (argsName.nonEmpty) argsName else other.argsName
  )
}

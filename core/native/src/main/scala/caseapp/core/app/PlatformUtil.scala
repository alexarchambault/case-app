package caseapp.core.app

object PlatformUtil {
  def exit(code: Int): Nothing =
    sys.exit(code)
  def arguments(args: Array[String]): Array[String] =
    args
}

package caseapp

object TestResources {
  private lazy val dir =
    Option(System.getenv("CASEAPP_NATIVE_TESTS_RESOURCES"))
      .map(os.Path(_))
      .getOrElse {
        sys.error("CASEAPP_NATIVE_TESTS_RESOURCES not set")
      }
  def path(name: String): String =
    (dir / name).toString
}

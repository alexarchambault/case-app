
plugins_(
  "com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"              % "3.0.0",
  "com.geirsson"                      % "sbt-ci-release"                % "1.5.2",
  "org.scala-js"                      % "sbt-scalajs"                   % "1.0.1",
  "org.portable-scala"                % "sbt-scalajs-crossproject"      % "1.0.0"
)


def plugins_(deps: ModuleID*) =
  deps.flatMap(addSbtPlugin(_))

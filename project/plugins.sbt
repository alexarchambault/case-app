
plugins_(
  "io.get-coursier"  % "sbt-coursier"             % "1.0.0-RC6",
  "com.jsuereth"     % "sbt-pgp"                  % "1.0.0",
  "org.scala-js"     % "sbt-scalajs"              % "0.6.16",
  "org.scala-native" % "sbt-scalajs-crossproject" % "0.1.0",
  "com.dwijnand"     % "sbt-travisci"             % "1.1.0",
  "org.tpolecat"     % "tut-plugin"               % "0.5.2"
)


def plugins_(deps: ModuleID*) =
  deps.flatMap(addSbtPlugin(_))

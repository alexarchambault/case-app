
plugins_(
  "io.get-coursier" % "sbt-coursier" % "1.0.0-RC6",
  "com.jsuereth"    % "sbt-pgp"      % "1.0.0",
  "org.scala-js"    % "sbt-scalajs"  % "0.6.15",
  "com.dwijnand"    % "sbt-travisci" % "1.1.0",
  "org.tpolecat"    % "tut-plugin"   % "0.4.8"
)


def plugins_(deps: ModuleID*) =
  deps.flatMap(addSbtPlugin(_))


plugins_(
  "com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"         % "1.1.0",
  "io.get-coursier"                   % "sbt-coursier"             % "1.0.0-RC10",
  "com.47deg"                         % "sbt-microsites"           % "0.6.1",
  "com.jsuereth"                      % "sbt-pgp"                  % "1.0.0",
  "org.scala-js"                      % "sbt-scalajs"              % "0.6.19",
  "org.scala-native"                  % "sbt-scalajs-crossproject" % "0.1.0",
  "org.scala-native"                  % "sbt-scala-native"         % "0.3.1",
  "com.dwijnand"                      % "sbt-travisci"             % "1.1.0",
  "com.eed3si9n"                      % "sbt-unidoc"               % "0.4.0",
  "org.tpolecat"                      % "tut-plugin"               % "0.5.2"
)


def plugins_(deps: ModuleID*) =
  deps.flatMap(addSbtPlugin(_))

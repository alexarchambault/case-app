
plugins_(
  "com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"              % "3.0.0",
  "com.geirsson"                      % "sbt-ci-release"                % "1.4.31",
  "org.scala-js"                      % "sbt-scalajs"                   % "0.6.29",
  "org.portable-scala"                % "sbt-scalajs-crossproject"      % "0.6.1",
  "org.portable-scala"                % "sbt-scala-native-crossproject" % "0.6.1",
  "org.scala-native"                  % "sbt-scala-native"              % "0.3.9",
  "com.eed3si9n"                      % "sbt-unidoc"                    % "0.4.2",
  "org.tpolecat"                      % "tut-plugin"                    % "0.6.12"
)


def plugins_(deps: ModuleID*) =
  deps.flatMap(addSbtPlugin(_))

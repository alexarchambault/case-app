
plugins_(
  "com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"              % "3.0.0",
  "com.geirsson"                      % "sbt-ci-release"                % "1.2.6",
  "com.47deg"                         % "sbt-microsites"                % "0.9.1",
  "com.jsuereth"                      % "sbt-pgp"                       % "1.1.2",
  "org.scala-js"                      % "sbt-scalajs"                   % "0.6.28",
  "org.portable-scala"                % "sbt-scalajs-crossproject"      % "0.6.0",
  "org.portable-scala"                % "sbt-scala-native-crossproject" % "0.6.0",
  "org.scala-native"                  % "sbt-scala-native"              % "0.3.9",
  "com.eed3si9n"                      % "sbt-unidoc"                    % "0.4.2",
  "org.tpolecat"                      % "tut-plugin"                    % "0.6.12"
)

addSbtCoursier


def plugins_(deps: ModuleID*) =
  deps.flatMap(addSbtPlugin(_))

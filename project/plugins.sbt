
plugins_(
  "com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"              % "2.0.1",
  "com.geirsson"                      % "sbt-ci-release"                % "1.2.2",
  "com.47deg"                         % "sbt-microsites"                % "0.7.13",
  "com.jsuereth"                      % "sbt-pgp"                       % "1.1.2",
  "org.scala-js"                      % "sbt-scalajs"                   % "0.6.26",
  "org.portable-scala"                % "sbt-scalajs-crossproject"      % "0.6.0",
  "org.portable-scala"                % "sbt-scala-native-crossproject" % "0.6.0",
  "org.scala-native"                  % "sbt-scala-native"              % "0.3.8",
  "com.eed3si9n"                      % "sbt-unidoc"                    % "0.4.2",
  "org.tpolecat"                      % "tut-plugin"                    % "0.6.10"
)

addSbtCoursier


def plugins_(deps: ModuleID*) =
  deps.flatMap(addSbtPlugin(_))

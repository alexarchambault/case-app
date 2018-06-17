
plugins_(
  "com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"              % "2.0.1",
  "com.47deg"                         % "sbt-microsites"                % "0.7.13",
  "com.jsuereth"                      % "sbt-pgp"                       % "1.1.1",
  "org.scala-js"                      % "sbt-scalajs"                   % "0.6.23",
  "org.portable-scala"                % "sbt-scalajs-crossproject"      % "0.5.0",
  "org.portable-scala"                % "sbt-scala-native-crossproject" % "0.5.0",
  "org.scala-native"                  % "sbt-scala-native"              % "0.3.7",
  "com.dwijnand"                      % "sbt-travisci"                  % "1.1.1",
  "com.eed3si9n"                      % "sbt-unidoc"                    % "0.4.2",
  "org.tpolecat"                      % "tut-plugin"                    % "0.6.4"
)

addSbtCoursier


def plugins_(deps: ModuleID*) =
  deps.flatMap(addSbtPlugin(_))

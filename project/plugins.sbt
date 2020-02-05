
plugins_(
  "com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"              % "3.0.0",
  "com.geirsson"                      % "sbt-ci-release"                % "1.5.0",
  "org.scala-js"                      % "sbt-scalajs"                   % "1.0.0",
  "org.portable-scala"                % "sbt-scalajs-crossproject"      % "0.6.1",
  "com.eed3si9n"                      % "sbt-unidoc"                    % "0.4.3",
  "org.tpolecat"                      % "tut-plugin"                    % "0.6.13"
)


def plugins_(deps: ModuleID*) =
  deps.flatMap(addSbtPlugin(_))

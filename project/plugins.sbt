resolvers += Resolver.url(
  "tpolecat-sbt-plugin-releases",
  url("http://dl.bintray.com/content/tpolecat/sbt-plugin-releases") )(
  Resolver.ivyStylePatterns
)

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.4.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.8")

addSbtPlugin("com.github.alexarchambault" % "coursier-sbt-plugin" % "1.0.0-M9")

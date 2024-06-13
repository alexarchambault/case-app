# Setup

Depend on case-app via `com.github.alexarchambault::case-app:@VERSION@`.
The latest version is [![Maven Central](https://img.shields.io/maven-central/v/com.github.alexarchambault/case-app_3.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.alexarchambault/case-app_3).

## JVM

```scala mdoc:invisible
val isSnapshot = "@VERSION@".endsWith("SNAPSHOT")
```

From [Mill](https://github.com/com-lihaoyi/Mill):
```scala mdoc:passthrough
def millMaybeAddSonatypeSnapshots() =
  if (isSnapshot)
    println(
      """def repositoriesTask = T {
        |  super.repositoriesTask() ++ Seq(
        |    coursier.Repositories.sonatype("snapshots")
        |  )
        |}""".stripMargin
    )
println("```scala")
millMaybeAddSonatypeSnapshots()
println(
  """def ivyDeps = Agg(
    |  ivy"com.github.alexarchambault::case-app:@VERSION@"
    |)""".stripMargin
)
println("```")
```

From [Scala CLI](https://github.com/VirtusLab/scala-cli):
```scala mdoc:passthrough
def scalaCliMaybeAddSonatypeSnapshots() =
  if (isSnapshot)
    println("//> using repository sonatype:snapshots")
println("```scala")
scalaCliMaybeAddSonatypeSnapshots()
println("//> using dep com.github.alexarchambault::case-app:@VERSION@")
println("```")
```

From [sbt](https://github.com/sbt/sbt):
```scala mdoc:passthrough
def sbtMaybeAddSonatypeSnapshots() =
  if (isSnapshot)
    println("""resolvers ++= Resolver.sonatypeOssRepos("snapshots")""")
println("```scala")
sbtMaybeAddSonatypeSnapshots()
println("""libraryDependencies += "com.github.alexarchambault" %% "case-app" % "@VERSION@"""")
println("```")
```

## Scala.js and Scala Native

Scala.js and Scala Native dependencies need to be marked as platform-specific, usually
[with an extra `:` or `%`](https://youforgotapercentagesignoracolon.com).

From [Mill](https://github.com/com-lihaoyi/Mill):
```scala mdoc:passthrough
println("```scala")
millMaybeAddSonatypeSnapshots()
println(
  """def ivyDeps = Agg(
    |  ivy"com.github.alexarchambault::case-app::@VERSION@"
    |)""".stripMargin
)
println("```")
```

From [Scala CLI](https://github.com/VirtusLab/scala-cli):
```scala mdoc:passthrough
println("```scala")
scalaCliMaybeAddSonatypeSnapshots()
println("//> using dep com.github.alexarchambault::case-app::@VERSION@")
println("```")
```

From [sbt](https://github.com/sbt/sbt):
```scala mdoc:passthrough
println("```scala")
sbtMaybeAddSonatypeSnapshots()
println("""libraryDependencies += "com.github.alexarchambault" %%% "case-app" % "@VERSION@"""")
println("```")
```

## Imports

Most case-app classes that are of relevance for end-users have aliases in the
`caseapp` package object. Importing its content is usually fine to use most
case-app features:
```scala mdoc:reset
import caseapp._
```

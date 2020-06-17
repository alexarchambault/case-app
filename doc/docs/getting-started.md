---
id: getting-started
title: Getting started
---

Add to your `build.sbt`

```scala
resolvers += Resolver.sonatypeRepo("releases")
libraryDependencies += "com.github.alexarchambault" %% "case-app" % "@VERSION@"
// cats-effect module
libraryDependencies += "com.github.alexarchambault" %% "case-app-cats" % "@VERSION@"
```

The latest version is [![Maven Central](https://img.shields.io/maven-central/v/com.github.alexarchambault/case-app_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.alexarchambault/case-app_2.13).

Note that case-app depends on shapeless 2.3. Use the `1.0.0` version if you depend on shapeless 2.2.

It is built against scala 2.12, and 2.13, and supports Scala.js too.

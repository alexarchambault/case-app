# Development

case-app is built with [Mill](https://mill-build.org), via the `./mill` launcher
at the root of the repository. The build is defined in `build.mill`.

Work in a full clone of the repository, with its git tags (not a shallow clone): the
version of the modules is computed from the latest `v*` tag, and the binary
compatibility checks compare the modules with the versions of the former tags.

## Modules

| Module        | Artifact               | Contents |
|---------------|------------------------|----------|
| `annotations` | `case-app-annotations` | Annotations of options (`@Name`, `@HelpMessage`, …) |
| `util`        | none                   | Scala 2 macros used by `core`, bundled in the `case-app` JAR (not published on their own) |
| `core`        | `case-app`             | The library itself: parsers, help messages, commands, completion |
| `cats`        | `case-app-cats`        | [cats-effect](misc.md) integration (`IOCaseApp`, `IOCommand`, …) |
| `tests`       | none                   | Tests of `annotations` and `core` (the definitions and demo apps they use are in its main sources). The tests of `cats` are in `cats` itself |
| `docs`        | none                   | This website: mdoc runs on `docs/pages`, that can use the helpers in `docs/src` (see [Website](contrib-website.md)) |

Modules are cross-built:

- `annotations` and `core` are built for Scala 2.13 and Scala 3 on the JVM, and for Scala 3 only on
  Scala.js and Scala Native,
- `cats` is built for Scala 3 only, on the JVM, Scala.js and Scala Native.

The exact Scala versions are in the `Versions` object of `build.mill` (`./mill show ci.scalaVersions`
prints them). Platform-specific modules are named after their platform (`core.jvm`, `core.js`,
`core.native`), and take a Scala version, like `core.jvm[2.13.18]`.

Sources live in the `src/main` and `src/test` directories of:

- `<module>/shared`: sources shared by all platforms,
- `<module>/jvm-native`: sources shared by the JVM and Scala Native,
- `<module>/jvm`, `<module>/js`, `<module>/native`: platform-specific sources,

with Scala version-specific sources in `scala-2`, `scala-3`, … directories next to the `scala` ones.

## Compiling

Compile everything with
```text
$ ./mill -i __.compile
```

or a single module, like
```text
$ ./mill -i 'core.jvm[3.3.9].compile'
```

## Running tests

Run all the tests with
```text
$ ./mill -i __.test
```

The CI runs the tests of each Scala version separately, like
```text
$ ./mill -i '__[3.3.9].test'
```

Run the tests of a single module with
```text
$ ./mill -i 'tests.jvm[2.13.18].test'
```

and those of a single test suite with
```text
$ ./mill -i 'tests.jvm[3.3.9].test.testOnly' caseapp.HelpTests
```

Tests use [utest](https://github.com/com-lihaoyi/utest). The tests of the `tests.js` and `cats.js`
modules need [Node.js](https://nodejs.org), and those of the Scala Native modules need the
[Scala Native requirements](https://scala-native.org/en/stable/user/setup.html) (clang, …).

## Formatting

Code is formatted with [scalafmt](https://scalameta.org/scalafmt), configured in `.scalafmt.conf`.
The CI checks that files are formatted with
```text
$ scalafmt --check
```

Run `scalafmt` (installed with `cs install scalafmt` for example) to format all files.

## Binary compatibility

The published modules are checked for binary compatibility with their former versions with
[MiMa](https://github.com/lightbend-labs/mima), like the CI does with
```text
$ ./mill -i __.mimaReportBinaryIssues
```

If an incompatibility is intended, add a filter to `mimaBinaryIssueFilters` in `build.mill`.

## Releases

Releases are made by pushing a `v*` tag: the CI then uploads the modules to Sonatype Central
(from where they're released to Maven Central), and updates this website. Snapshots are
published on each push to `main`.

The changes of each version are listed in the
[GitHub releases](https://github.com/alexarchambault/case-app/releases).

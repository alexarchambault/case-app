# Unroll Fork

`unroll` contains a locally vendored fork of the upstream `com-lihaoyi/unroll` compiler plugin.

Upstream project:
- https://github.com/com-lihaoyi/unroll

Licensing:
- The code under `unroll` retains the upstream MIT license.
- See `unroll/LICENSE` for the license text.

Local use in this repository:
- This fork is built by `build.mill` (`unroll`) as a compiler plugin, for Scala 3 only, and is only
  used to compile case-app itself. It is not published.
- It replaces the [data-class](https://github.com/alexarchambault/data-class) macros on Scala 3,
  which keep being used on Scala 2: it generates the binary-compatible `apply` / `copy` /
  constructor overloads that data-class generates on Scala 2 for fields annotated with `@since`.
- The `@data` and `@since` annotations of the shared sources are Scala 3 stand-ins on that side,
  defined in core (`core/shared/src/main/scala-3/dataclass`). `@data` is a no-op there, and the
  plugin handles `@since` like `@com.lihaoyi.unroll`.

Provenance:
- This subtree was copied from the vendored copy in
  [coursier/coursier](https://github.com/coursier/coursier) (`modules/unroll/plugin/src-3`), itself
  imported from [chipsalliance/chisel](https://github.com/chipsalliance/chisel), itself imported
  from upstream commit c9fd7e1ce9a92fe4ab15cd12fb61facc3aed8f2c (tag 0.3.0).
- Modifications in coursier, kept here (`UnrollPhaseScala3.scala`):
  - only the compiler-generated `apply` / `fromProduct` / `copy` of case classes (flagged
    `Synthetic`) are special-cased, so that hand-written `apply` overloads in case class
    companions no longer make the plugin fail with "wrong number of arguments at unroll2";
  - a forwarder is not generated when an overload with the same signature is defined by hand
    (the shorter forwarders call that overload instead), so that hand-written overloads take
    precedence rather than clashing with generated ones.
- Modifications in case-app:
  - `dataclass.since` is accepted in addition to `com.lihaoyi.unroll`. In coursier, `since` is a type
    alias of the latter, but case-app already published a `dataclass.since` class in its Scala 3
    JARs, which has to be kept for binary compatibility;
  - the two phases coursier adds to implement data-class options (`cachedHashCode`,
    `settersCallApply`, `copy` of classes with an implicit parameter list) are left out, as
    case-app doesn't use those.

Setters (`withX`):
- The plugin generates none, while the data-class macro generates one per field on Scala 2, where
  they're deprecated. Sources call `copy` instead, which both Scala 2 and Scala 3 provide.

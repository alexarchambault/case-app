package caseapp
package demo

sealed abstract class ManualCommandOptions extends Product with Serializable

@ProgName("c1")
@ArgsName("c1-stuff")
final case class Command1Opts(s: String) extends ManualCommandOptions

@ProgName("c2")
final case class Command2Opts(b: Boolean) extends ManualCommandOptions

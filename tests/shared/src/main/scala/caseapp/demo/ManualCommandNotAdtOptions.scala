package caseapp.demo

import caseapp.{ArgsName, ProgName}

object ManualCommandNotAdtOptions {

  @ProgName("c1")
  @ArgsName("c1-stuff")
  final case class Command1Opts(s: String)

  @ProgName("c2")
  final case class Command2Opts(b: Boolean)

}

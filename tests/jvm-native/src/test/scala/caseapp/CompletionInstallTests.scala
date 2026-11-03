package caseapp

import utest._

object CompletionInstallTests extends TestSuite {

  private def shellTest(
    shell: String,
    expectedFiles: Seq[(os.SubPath, String)]
  ): Unit = {
    val shell0 = shell
    val dir    = os.temp.dir(prefix = "case-app-test")
    try {
      val prog = new CommandsEntryPoint {
        override def completionHome           = dir.toNIO
        override def shell                    = Some(shell0)
        override def completionXdgHome        = None
        override def completionZDotDir        = None
        override def enableCompletionsCommand = true
        def progName                          = "prog"
        def commands                          = Seq(
          CompletionDefinitions.Commands.First,
          CompletionDefinitions.Commands.Second,
          CompletionDefinitions.Commands.BackTick
        )
      }

      prog.main(Array("completions", "install"))
      def listFiles() = os.walk(dir)
        .filter(!os.isDir(_))
        .map { f =>
          val relPath = f.relativeTo(dir).asSubPath
          val content = os.read(f).replaceAll(dir.toString, "\\$HOME")
          (relPath, content)
        }
        .sortBy(_._1.toString)
      val files          = listFiles()
      val expectedFiles0 = expectedFiles.sortBy(_._1.toString)
      if (files != expectedFiles0) {
        pprint.err.log(expectedFiles0)
        pprint.err.log(files)
      }
      assert(files == expectedFiles0)

      prog.main(Array("completions", "uninstall"))
      val remaining = listFiles().filter(_._2.nonEmpty)
      if (!remaining.isEmpty)
        pprint.err.log(remaining)
      assert(remaining.isEmpty)
    }
    finally os.remove.all(dir)
  }

  val tests = Tests {
    test("zsh") {
      shellTest(
        "zsh",
        Seq(
          os.sub / ".config" / "zsh" / "completions" / "_prog" ->
            """#compdef _prog prog
              |
              |function _prog {
              |  eval "$(prog complete zsh-v1 $CURRENT $words[@])"
              |}
              |""".stripMargin,
          os.sub / ".zshrc" ->
            """
              |# >>> prog completions >>>
              |fpath=("$HOME/.config/zsh/completions" $fpath)
              |compinit
              |# <<< prog completions <<<
              |""".stripMargin
        )
      )
    }

    test("bash") {
      shellTest(
        "bash",
        Seq(
          os.sub / ".bashrc" ->
            """
              |# >>> prog completions >>>
              |_prog_completions() {
              |  local IFS=$'\n'
              |  eval "$(prog complete bash-v1 "$(( $COMP_CWORD + 1 ))" "${COMP_WORDS[@]}")"
              |}
              |
              |complete -F _prog_completions prog
              |# <<< prog completions <<<
              |""".stripMargin
        )
      )
    }

    test("fish") {
      shellTest(
        "fish",
        Seq(
          os.sub / ".config" / "fish" / "completions" / "prog.fish" ->
            """
              |# >>> prog completions >>>
              |complete prog -a '(prog complete fish-v1 (math 1 + (count (__fish_print_cmd_args))) (__fish_print_cmd_args))'
              |# <<< prog completions <<<
              |""".stripMargin
        )
      )
    }
  }
}

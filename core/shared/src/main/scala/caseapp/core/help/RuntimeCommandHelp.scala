package caseapp.core.help

import dataclass.data

@data class RuntimeCommandHelp[T](
  names: List[List[String]],
  help: Help[T],
  group: String,
  hidden: Boolean
)

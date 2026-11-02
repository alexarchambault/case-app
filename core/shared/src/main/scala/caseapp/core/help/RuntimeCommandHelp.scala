package caseapp.core.help

import dataclass.data

@data(
  deprecatedSetters = true,
  deprecatedSettersMessage = "Use copy instead",
  deprecatedSettersSince = "2.1.2"
) case class RuntimeCommandHelp[T](
  names: List[List[String]],
  help: Help[T],
  group: String,
  hidden: Boolean
)

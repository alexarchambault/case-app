package caseapp

/**
  * Core types / classes of caseapp.
  *
  * Not that in most use cases of caseapp, simply importing things right under [[caseapp]], rather things from
  * [[caseapp.core]], should be enough.
  *
  *
  * This package is itself split in several sub-packages:
  * - [[caseapp.core.argparser]]: things related to parsing a single argument value,
  * - [[caseapp.core.parser]]: things related to parsing a sequence of arguments,
  * - [[caseapp.core.commandparser]]: things related to parsing a sequence of arguments, handling commands,
  * - [[caseapp.core.help]]: things related to help messages,
  * - [[caseapp.core.app]]: helpers to create caseapp-based applications,
  * - [[caseapp.core.default]]: helper to set / define a default value for a given type,
  * - [[caseapp.core.util]]: utilities, mostly for internal use.
  */
package object core

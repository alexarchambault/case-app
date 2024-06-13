package caseapp.core.commandparser

import caseapp.core.app.{CaseApp, Command}

import scala.annotation.tailrec
import scala.collection.mutable
import caseapp.core.complete.CompletionItem

object RuntimeCommandParser {

  def parse[T](
    apps: Map[List[String], T],
    args: List[String]
  ): Option[(List[String], T, List[String])] = {
    val tree = CommandTree.fromCommandMap(apps)
    tree.command(args)
  }

  def parse[T](
    defaultApp: T,
    apps: Map[List[String], T],
    args: List[String]
  ): (List[String], T, List[String]) = {
    val tree = CommandTree.fromCommandMap(apps)
    tree.command(args).getOrElse((Nil, defaultApp, args))
  }

  private def commandMap(commands: Seq[Command[_]]): Map[List[String], Command[_]] =
    commands.flatMap(cmd =>
      cmd.names.map(names => names -> cmd): Seq[(List[String], Command[_])]
    ).toMap

  def parse(
    commands: Seq[Command[_]],
    args: List[String]
  ): Option[(List[String], Command[_], List[String])] = {
    val map  = commandMap(commands)
    val tree = CommandTree.fromCommandMap(map)
    tree.command(args)
  }

  def parse(
    defaultCommand: Command[_],
    commands: Seq[Command[_]],
    args: List[String]
  ): (List[String], Command[_], List[String]) = {
    val map  = commandMap(commands)
    val tree = CommandTree.fromCommandMap(map)
    tree.command(args).getOrElse((Nil, defaultCommand, args))
  }

  def complete(
    defaultCommand: Command[_],
    commands: Seq[Command[_]],
    args: List[String],
    index: Int
  ): List[CompletionItem] = {
    val map  = commandMap(commands)
    val tree = CommandTree.fromCommandMap(map)
    val (commandName, command, commandArgs) =
      tree.command(args).getOrElse((Nil, defaultCommand, args))
    val prefix                 = args.applyOrElse(index, (_: Int) => "")
    val commandNameCompletions = tree.complete(args.take(index)).flatMap(_.withPrefix(prefix).toSeq)
    val commandCompletions =
      if (index < commandName.length) Nil
      else
        command.complete(
          commandArgs,
          index - commandName.length
        )
    commandNameCompletions ++ commandCompletions
  }

  def complete(
    commands: Seq[Command[_]],
    args: List[String],
    index: Int
  ): List[CompletionItem] = {
    val map                    = commandMap(commands)
    val tree                   = CommandTree.fromCommandMap(map)
    val prefix                 = args.applyOrElse(index, (_: Int) => "")
    val commandNameCompletions = tree.complete(args.take(index)).flatMap(_.withPrefix(prefix).toSeq)
    val commandArgsCompletions = tree.command(args).toList.flatMap {
      case (commandName, command, commandArgs) =>
        if (index < commandName.length) Nil
        else
          command.complete(
            commandArgs,
            index - commandName.length
          )
    }
    commandNameCompletions ++ commandArgsCompletions
  }

  private final case class CommandTree[T](defaultApp: Option[T], map: Map[String, CommandTree[T]]) {

    @tailrec
    def complete(prefix: List[String])(implicit ev: T <:< CaseApp[_]): List[CompletionItem] =
      prefix match {
        case Nil =>
          val byApps = map.toList.groupBy(_._2.defaultApp)
          byApps.toList.sortBy(_._2.head._1).map {
            case (appOpt, values) =>
              val values0 = values.map(_._1)
              CompletionItem(
                values0.head,
                appOpt.map(ev).flatMap(_.messages.helpMessage.map(_.message)),
                values0.tail
              )
          }
        case h :: t =>
          map.get(h) match {
            case None => Nil
            case Some(subTree) =>
              subTree.complete(t)
          }
      }

    def command(args: List[String]): Option[(List[String], T, List[String])] =
      command(args, Nil)

    def command(
      args: List[String],
      reverseName: List[String]
    ): Option[(List[String], T, List[String])] =
      args match {
        case Nil => defaultApp.map((Nil, _, args))
        case h :: t =>
          map.get(h) match {
            case None =>
              if (reverseName.isEmpty) defaultApp.map((Nil, _, reverseName.reverse ::: args))
              else None
            case Some(tree0) =>
              val reverseName0 = h :: reverseName
              lazy val current = tree0.defaultApp.map((reverseName0.reverse, _, t))
              if (t.isEmpty) current
              else tree0.command(t, reverseName0).orElse(current)
          }
      }
  }

  private object CommandTree {
    private final case class Mutable[T](
      var value: Option[T] = None,
      map: mutable.HashMap[String, Mutable[T]] = new mutable.HashMap[String, Mutable[T]]
    ) {
      @tailrec
      def add(command: List[String], parser: T): Unit =
        command match {
          case Nil =>
            value = Some(parser)
          case h :: t =>
            map
              .getOrElseUpdate(h, Mutable[T]())
              .add(t, parser)
        }

      def add(commandMap: Map[List[String], T]): this.type = {
        for ((c, p) <- commandMap)
          add(c, p)
        this
      }

      def result: CommandTree[T] = {
        val map0 = map
          .iterator
          .map {
            case (name, mutable0) =>
              (name, mutable0.result)
          }
          .toMap
        CommandTree(value, map0)
      }
    }

    def fromCommandMap[T](commandMap: Map[List[String], T]): CommandTree[T] =
      Mutable[T]().add(commandMap).result
  }

}

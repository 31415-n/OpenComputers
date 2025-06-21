package li.cil.oc.common.command

import java.util

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.MinecraftServer

import scala.jdk.CollectionConverters._
import scala.collection.mutable

abstract class SimpleCommand(val name: String) {
  protected var aliases = mutable.ListBuffer.empty[String]

  def getName: String = name

  def getAliases: util.List[String] = aliases.asJava

  def register(dispatcher: CommandDispatcher[CommandSourceStack]): Unit = {
    // Modern command registration will be implemented by subclasses
  }

  def execute(context: CommandContext[CommandSourceStack]): Int = {
    // Default implementation - subclasses should override
    0
  }
}

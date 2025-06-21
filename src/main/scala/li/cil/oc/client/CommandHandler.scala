package li.cil.oc.client

import li.cil.oc.common.command.SimpleCommand
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandSource
import net.minecraft.server.MinecraftServer
import net.minecraftforge.client.event.RegisterClientCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext

@Mod.EventBusSubscriber(modid = "opencomputers", bus = Mod.EventBusSubscriber.Bus.FORGE)
object CommandHandler {
  
  @SubscribeEvent
  def onRegisterClientCommands(event: RegisterClientCommandsEvent): Unit = {
    val dispatcher = event.getDispatcher
    dispatcher.register(
      LiteralArgumentBuilder.literal[CommandSource]("oc_setclipboard")
        .`then`(
          com.mojang.brigadier.builder.RequiredArgumentBuilder.argument[CommandSource, String]("value", StringArgumentType.greedyString())
            .executes((context: CommandContext[CommandSource]) => {
              val value = StringArgumentType.getString(context, "value")
              if (context.getSource.getLevel != null && context.getSource.getLevel.isClientSide) {
                Minecraft.getInstance().keyboardHandler.setClipboard(value)
              }
              1
            })
        )
    )
  }}

package li.cil.oc.client

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandSourceStack
import net.minecraftforge.client.event.RegisterClientCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = "opencomputers", bus = Mod.EventBusSubscriber.Bus.FORGE)
object CommandHandler {
  
  @SubscribeEvent
  def onRegisterClientCommands(event: RegisterClientCommandsEvent): Unit = {
    val dispatcher: CommandDispatcher[CommandSourceStack] = event.getDispatcher
    
    dispatcher.register(
      LiteralArgumentBuilder.literal[CommandSourceStack]("oc_setclipboard")
        .`then`(
          com.mojang.brigadier.builder.RequiredArgumentBuilder.argument[CommandSourceStack, String]("value", StringArgumentType.greedyString())
            .executes((context: CommandContext[CommandSourceStack]) => {
              val value = StringArgumentType.getString(context, "value")
              // Check if we're on client side by verifying player exists and level is client side
              if (context.getSource.isPlayer && context.getSource.getPlayer.level().isClientSide) {
                Minecraft.getInstance().keyboardHandler.setClipboard(value)
              }
              1 // Return success code
            })
        )
    )
  }
}

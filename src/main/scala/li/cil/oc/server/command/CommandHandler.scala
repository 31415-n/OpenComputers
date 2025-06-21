package li.cil.oc.server.command

import net.minecraftforge.event.server.ServerStartingEvent
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack

object CommandHandler {
  def register(e: ServerStartingEvent): Unit = {
    val dispatcher = e.getServer.getCommands.getDispatcher
    // Modern command registration using Brigadier
    DebugNanomachinesCommand.register(dispatcher)
    LogNanomachinesCommand.register(dispatcher)
    NonDisassemblyAgreementCommand.register(dispatcher)
    WirelessRenderingCommand.register(dispatcher)
    SpawnComputerCommand.register(dispatcher)
    DebugWhitelistCommand.register(dispatcher)
    SendDebugMessageCommand.register(dispatcher)
  }
}

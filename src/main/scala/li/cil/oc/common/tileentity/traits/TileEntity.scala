package li.cil.oc.common.tileentity.traits

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.client.Sound
import li.cil.oc.common.SaveHandler
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.SideTracker
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

trait TileEntity extends net.minecraft.world.level.block.entity.BlockEntity {
  private final val IsServerDataTag = Settings.namespace + "isServerData"

  def x: Int = getBlockPos.getX

  def y: Int = getBlockPos.getY

  def z: Int = getBlockPos.getZ

  def position = BlockPosition(x, y, z, getLevel)

  def isClient: Boolean = !isServer

  def isServer: Boolean = if (getLevel != null) !getLevel.isClientSide else SideTracker.isServer

  // ----------------------------------------------------------------------- //

  def updateEntity(): Unit = {
    if (Settings.get.periodicallyForceLightUpdate && getLevel.getGameTime % 40 == 0 && getBlockState.getLightEmission > 0) {
      getLevel.sendBlockUpdated(getBlockPos, getBlockState, getBlockState, 3)
    }
  }

  override def onLoad(): Unit = {
    super.onLoad()
    initialize()
  }

  override def setRemoved(): Unit = {
    super.setRemoved()
    dispose()
  }

  def onChunkUnload(): Unit = {
    try dispose() catch {
      case t: Throwable => OpenComputers.log.error("Failed properly disposing a tile entity, things may leak and or break.", t)
    }
  }

  protected def initialize(): Unit = {
  }

  def dispose(): Unit = {
    if (isClient) {
      // Note: chunk unload is handled by sound via event handler.
      Sound.stopLoop(this)
    }
  }

  // ----------------------------------------------------------------------- //

  def shouldRefresh(world: Level, pos: BlockPos, oldState: BlockState, newState: BlockState): Boolean = oldState.getBlock != newState.getBlock

  def readFromNBTForServer(nbt: CompoundTag): Unit = load(nbt)

  def writeToNBTForServer(nbt: CompoundTag): Unit = {
    nbt.putBoolean(IsServerDataTag, true)
    saveAdditional(nbt)
  }

  @OnlyIn(Dist.CLIENT)
  def readFromNBTForClient(nbt: CompoundTag): Unit = {}

  def writeToNBTForClient(nbt: CompoundTag): Unit = {
    nbt.putBoolean(IsServerDataTag, false)
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: CompoundTag): Unit = {
    super.load(nbt)
    if (isServer || nbt.getBoolean(IsServerDataTag)) {
      readFromNBTForServer(nbt)
    }
    else {
      readFromNBTForClient(nbt)
    }
  }

  override def saveAdditional(nbt: CompoundTag): Unit = {
    super.saveAdditional(nbt)
    if (isServer) {
      writeToNBTForServer(nbt)
    }
  }

  override def getUpdatePacket: ClientboundBlockEntityDataPacket = {
    ClientboundBlockEntityDataPacket.create(this)
  }

  override def getUpdateTag: CompoundTag = {
    val nbt = super.getUpdateTag

    // See comment on savingForClients variable.
    SaveHandler.savingForClients = true
    try {
      try writeToNBTForClient(nbt) catch {
        case e: Throwable => OpenComputers.log.warn("There was a problem writing a TileEntity description packet. Please report this if you see it!", e)
      }
    } finally {
      SaveHandler.savingForClients = false
    }

    nbt
  }

  override def onDataPacket(manager: Connection, packet: ClientboundBlockEntityDataPacket): Unit = {
    try readFromNBTForClient(packet.getTag) catch {
      case e: Throwable => OpenComputers.log.warn("There was a problem reading a TileEntity description packet. Please report this if you see it!", e)
    }
  }
}

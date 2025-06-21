package li.cil.oc.common

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import io.netty.buffer.Unpooled
import li.cil.oc.{OpenComputers, Settings}
import li.cil.oc.api.network.EnvironmentHost
import net.minecraft.world.entity.Entity
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.server.level.ServerLevel

import scala.jdk.CollectionConverters._

abstract class PacketBuilder(stream: OutputStream) extends DataOutputStream(stream) {
  def writeTileEntity(t: BlockEntity): Unit = {
    writeInt(t.getLevel.dimension().location().toString.hashCode)
    writeInt(t.getBlockPos.getX)
    writeInt(t.getBlockPos.getY)
    writeInt(t.getBlockPos.getZ)
  }

  def writeEntity(e: Entity): Unit = {
    writeInt(e.level.dimension().location().toString.hashCode)
    writeInt(e.getId)
  }

  def writeDirection(d: Option[Direction]) = d match {
    case Some(side) => writeByte(side.ordinal.toByte)
    case _ => writeByte(-1: Byte)
  }

  def writeItemStack(stack: ItemStack) = {
    val haveStack = !stack.isEmpty && stack.getCount > 0
    writeBoolean(haveStack)
    if (haveStack) {
      writeNBT(stack.save(new CompoundTag()))
    }
  }

  def writeNBT(nbt: CompoundTag) = {
    val haveNbt = nbt != null
    writeBoolean(haveNbt)
    if (haveNbt) {
      NbtIo.write(nbt, this)
    }
  }

  def writeMedium(v: Int) = {
    writeByte(v & 0xFF)
    writeByte((v >> 8) & 0xFF)
    writeByte((v >> 16) & 0xFF)
  }

  def writePacketType(pt: PacketType.Value) = writeByte(pt.id)

  def sendToAllPlayers() = {
    val server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer
    if (server != null) {
      val packetData = packet
      server.getPlayerList.getPlayers.asScala.foreach(player => {
        sendToPlayer(player)
      })
    }
  }

  def sendToPlayersNearEntity(e: Entity, range: Option[Double] = None): Unit = sendToNearbyPlayers(e.level, e.getX, e.getY, e.getZ, range)

  def sendToPlayersNearHost(host: EnvironmentHost, range: Option[Double] = None): Unit = {
    host match {
      case t: BlockEntity => sendToPlayersNearTileEntity(t, range)
      case _ => sendToNearbyPlayers(host.world, host.xPosition, host.yPosition, host.zPosition, range)
    }
  }

  def sendToPlayersNearTileEntity(t: BlockEntity, range: Option[Double] = None): Unit = {
    t.getLevel match {
      case w: ServerLevel =>
        val chunkX = t.getBlockPos.getX >> 4
        val chunkZ = t.getBlockPos.getZ >> 4

        val server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer
        val manager = server.getPlayerList
        var maxPacketRange = range.getOrElse((manager.getViewDistance + 1) * 16.0)
        val maxPacketRangeConfig = Settings.get.maxNetworkClientPacketDistance
        if (maxPacketRangeConfig > 0.0D) {
          maxPacketRange = maxPacketRange min maxPacketRangeConfig
        }
        val maxPacketRangeSq = maxPacketRange * maxPacketRange

        for (player <- w.players().asScala) {
          player match {
            case serverPlayer: ServerPlayer =>
              val chunkPos = new net.minecraft.world.level.ChunkPos(chunkX, chunkZ)
              if (w.getChunkSource.chunkMap.getPlayers(chunkPos, false).contains(serverPlayer)) {
                if (serverPlayer.distanceToSqr(t.getBlockPos.getX + 0.5D, t.getBlockPos.getY + 0.5D, t.getBlockPos.getZ + 0.5D) <= maxPacketRangeSq)
                  sendToPlayer(serverPlayer)
              }
            case _ =>
          }
        }
      case _ => sendToNearbyPlayers(t.getLevel, t.getBlockPos.getX + 0.5D, t.getBlockPos.getY + 0.5D, t.getBlockPos.getZ + 0.5D, range)
    }
  }

  def sendToNearbyPlayers(world: Level, x: Double, y: Double, z: Double, range: Option[Double]): Unit = {
    val dimensionKey = world.dimension()
    val server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer
    val manager = server.getPlayerList

    var maxPacketRange = range.getOrElse((manager.getViewDistance + 1) * 16.0)
    val maxPacketRangeConfig = Settings.get.maxNetworkClientPacketDistance
    if (maxPacketRangeConfig > 0.0D) {
      maxPacketRange = maxPacketRange min maxPacketRangeConfig
    }
    val maxPacketRangeSq = maxPacketRange * maxPacketRange

    for (player <- manager.getPlayers.asScala if player.level.dimension() == dimensionKey) {
      if (player.distanceToSqr(x, y, z) <= maxPacketRangeSq) {
        sendToPlayer(player)
      }
    }
  }

  def sendToPlayer(player: ServerPlayer): Unit = {
    // Create custom packet for OpenComputers networking
    val packetData = packet
    if (packetData != null) {
      // Use Forge's networking system to send custom packet
      val buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(packetData.asInstanceOf[Array[Byte]]))
      val customPacket = new net.minecraftforge.network.NetworkEvent.Context(() => player, () => net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
      // Send through OpenComputers channel
      OpenComputers.networkHandler.sendToPlayer(buf, player)
    }
  }

  def sendToServer(): Unit = {
    // Send packet to server using Forge networking
    val packetData = packet
    if (packetData != null) {
      val buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(packetData.asInstanceOf[Array[Byte]]))
      OpenComputers.networkHandler.sendToServer(buf)
    }
  }

  protected def packet: Array[Byte]
}

// Necessary to keep track of the GZIP stream.
abstract class PacketBuilderBase[T <: OutputStream](protected val stream: T) extends PacketBuilder(new BufferedOutputStream(stream))

class SimplePacketBuilder(val packetType: PacketType.Value) extends PacketBuilderBase(PacketBuilder.newData(compressed = false)) {
  writeByte(packetType.id)

  override protected def packet = {
    flush()
    stream.toByteArray
  }
}

class CompressedPacketBuilder(val packetType: PacketType.Value, private val data: ByteArrayOutputStream = PacketBuilder.newData(compressed = true)) extends PacketBuilderBase(new DeflaterOutputStream(data, new Deflater(Deflater.BEST_SPEED))) {
  writeByte(packetType.id)

  override protected def packet = {
    flush()
    stream.finish()
    data.toByteArray
  }
}

object PacketBuilder {
  def newData(compressed: Boolean) = {
    val data = new ByteArrayOutputStream
    data.write(if (compressed) 1 else 0)
    data
  }
}

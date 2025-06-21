package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.driver.DriverBlock
import li.cil.oc.api.internal
import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network._
import li.cil.oc.common.Slot
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import net.minecraft.world.entity.player.Player
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundSource
import net.minecraftforge.common.util.Constants.NBT

import scala.jdk.CollectionConverters._
import scala.collection.mutable

class Adapter extends traits.Environment with traits.ComponentInventory with traits.Tickable with traits.OpenSides with Analyzable with internal.Adapter with DeviceInfo {
  val node = api.Network.newNode(this, Visibility.Network).create()

  private val blocks = Array.fill[Option[(ManagedEnvironment, DriverBlock)]](6)(None)

  private val updatingBlocks = mutable.ArrayBuffer.empty[ManagedEnvironment]

  private val blocksData = Array.fill[Option[BlockData]](6)(None)

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Bus,
    DeviceAttribute.Description -> "Adapter",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Multiplug Ext.1"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  // ----------------------------------------------------------------------- //

  override protected def defaultState = true

  override def setSideOpen(side: Direction, value: Boolean) {
    super.setSideOpen(side, value)
    if (isServer) {
      ServerPacketSender.sendAdapterState(this)
      getWorld.playSound(null, x + 0.5, y + 0.5, z + 0.5, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f, getWorld.getRandom.nextFloat() * 0.25f + 0.7f)
      getWorld.updateNeighborsAt(getPos, getBlockState.getBlock)
      neighborChanged(side)
    } else {
      getWorld.sendBlockUpdated(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
    }
  }

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: Player, side: Direction, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = {
    (blocks collect {
      case Some((environment, _)) => environment.node
    }) ++
    (components collect {
      case Some(environment) => environment.node
    })
  }

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (isServer && updatingBlocks.nonEmpty) {
      for (block <- updatingBlocks) {
        block.update()
      }
    }
  }

  def neighborChanged(d: Direction) {
    if (node != null && node.network != null) {
      val blockPos = getPos.relative(d)
      getWorld.getBlockEntity(blockPos) match {
        case _: traits.Environment =>
        // Don't provide adaption for our stuffs. This is mostly to avoid
        // cables and other non-functional stuff popping up in the adapter
        // due to having a power interface. Might revisit this at some point,
        // but the only 'downside' is that it can't be used to manipulate
        // inventories, which I actually consider a plus :P
        case _ =>
          Option(api.Driver.driverFor(getWorld, blockPos, d)) match {
            case Some(newDriver) if isSideOpen(d) => blocks(d.ordinal()) match {
              case Some((oldEnvironment, driver)) =>
                if (newDriver != driver) {
                  // This is... odd. Maybe moved by some other mod? First, clean up.
                  blocks(d.ordinal()) = None
                  updatingBlocks -= oldEnvironment
                  blocksData(d.ordinal()) = None
                  node.disconnect(oldEnvironment.node)

                  // Then rebuild - if we have something.
                  val environment = newDriver.createEnvironment(getWorld, blockPos, d)
                  if (environment != null) {
                    blocks(d.ordinal()) = Some((environment, newDriver))
                    if (environment.canUpdate) {
                      updatingBlocks += environment
                    }
                    blocksData(d.ordinal()) = Some(new BlockData(environment.getClass.getName, new CompoundTag()))
                    node.connect(environment.node)
                  }
                } // else: the more things change, the more they stay the same.
              case _ =>
                if (!isSideOpen(d)) {
                  return
                }
                // A challenger appears. Maybe.
                val environment = newDriver.createEnvironment(getWorld, blockPos, d)
                if (environment != null) {
                  blocks(d.ordinal()) = Some((environment, newDriver))
                  if (environment.canUpdate) {
                    updatingBlocks += environment
                  }
                  blocksData(d.ordinal()) match {
                    case Some(data) if data.name == environment.getClass.getName =>
                      environment.load(data.data)
                    case _ =>
                  }
                  blocksData(d.ordinal()) = Some(new BlockData(environment.getClass.getName, new CompoundTag()))
                  node.connect(environment.node)
                }
            }
            case _ => blocks(d.ordinal()) match {
              case Some((environment, driver)) =>
                // We had something there, but it's gone now...
                node.disconnect(environment.node)
                environment.save(blocksData(d.ordinal()).get.data)
                Option(environment.node).foreach(_.remove())
                blocks(d.ordinal()) = None
                updatingBlocks -= environment
              case _ => // Nothing before, nothing now.
            }
          }
      }
    }
  }

  def neighborChanged() {
    if (node != null && node.network != null) {
      for (d <- Direction.values) {
        neighborChanged(d)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      neighborChanged()
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      updatingBlocks.clear()
    }
  }

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 1

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = (slot, Option(Driver.driverFor(stack, getClass))) match {
    case (0, Some(driver)) => driver.slot(stack) == Slot.Upgrade
    case _ => false
  }

  // ----------------------------------------------------------------------- //

  private final val BlocksTag = Settings.namespace + "adapter.blocks"
  private final val BlockNameTag = "name"
  private final val BlockDataTag = "data"

  override def readFromNBTForServer(nbt: CompoundTag) {
    super.readFromNBTForServer(nbt)

    val blocksNbt = nbt.getList(BlocksTag, NBT.TAG_COMPOUND)
    (0 until (blocksNbt.size min blocksData.length)).
      map(blocksNbt.getCompound).
      zipWithIndex.
      foreach {
        case (blockNbt, i) =>
          if (blockNbt.contains(BlockNameTag) && blockNbt.contains(BlockDataTag)) {
            blocksData(i) = Some(new BlockData(blockNbt.getString(BlockNameTag), blockNbt.getCompound(BlockDataTag)))
          }
      }
  }

  override def writeToNBTForServer(nbt: CompoundTag) {
    super.writeToNBTForServer(nbt)

    val blocksNbt = new ListTag()
    for (i <- blocks.indices) {
      val blockNbt = new CompoundTag()
      blocksData(i) match {
        case Some(data) =>
          blocks(i) match {
            case Some((environment, _)) => environment.save(data.data)
            case _ =>
          }
          blockNbt.putString(BlockNameTag, data.name)
          blockNbt.put(BlockDataTag, data.data)
        case _ =>
      }
      blocksNbt.add(blockNbt)
    }
    nbt.put(BlocksTag, blocksNbt)
  }

  // ----------------------------------------------------------------------- //

  private class BlockData(val name: String, val data: CompoundTag)

}

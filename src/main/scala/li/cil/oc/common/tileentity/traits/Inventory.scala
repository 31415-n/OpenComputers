package li.cil.oc.common.tileentity.traits

import li.cil.oc.common.inventory
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.InventoryUtils
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component

trait Inventory extends TileEntity with inventory.Inventory {
  private lazy val inventory = Array.fill[ItemStack](getContainerSize)(ItemStack.EMPTY)

  def items = inventory

  // ----------------------------------------------------------------------- //

  override def getDisplayName: Component = super[Inventory].getDisplayName

  override def loadForServer(nbt: CompoundTag): Unit = {
    super.loadForServer(nbt)
    load(nbt)
  }

  override def saveForServer(nbt: CompoundTag): Unit = {
    super.saveForServer(nbt)
    save(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def stillValid(player: Player) =
    player.distanceToSqr(getBlockPos.getX + 0.5, getBlockPos.getY + 0.5, getBlockPos.getZ + 0.5) <= 64

  // ----------------------------------------------------------------------- //

  def dropSlot(slot: Int, count: Int = getMaxStackSize, direction: Option[Direction] = None) =
    InventoryUtils.dropSlot(BlockPosition(getBlockPos.getX, getBlockPos.getY, getBlockPos.getZ, getLevel), this, slot, count, direction)

  def dropAllSlots() =
    InventoryUtils.dropAllSlots(BlockPosition(getBlockPos.getX, getBlockPos.getY, getBlockPos.getZ, getLevel), this)

  def spawnStackInWorld(stack: ItemStack, direction: Option[Direction] = None) =
    InventoryUtils.spawnStackInWorld(BlockPosition(getBlockPos.getX, getBlockPos.getY, getBlockPos.getZ, getLevel), stack, direction)
}

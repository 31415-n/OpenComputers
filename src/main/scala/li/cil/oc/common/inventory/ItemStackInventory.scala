package li.cil.oc.common.inventory

import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag

trait ItemStackInventory extends Inventory {
  // The item stack that provides the inventory.
  def container: ItemStack

  private lazy val inventory = Array.fill[ItemStack](getContainerSize)(ItemStack.EMPTY)

  override def items = inventory

  // Initialize the list automatically if we have a container.
  {
    val _container = container
    if (_container != null && !_container.isEmpty) {
      reinitialize()
    }
  }

  // Load items from tag.
  def reinitialize() {
    for (i <- items.indices) {
      updateItems(i, ItemStack.EMPTY)
    }
    if (!container.hasTag) {
      container.setTag(new CompoundTag())
    }
    load(container.getTag)
  }

  // Write items back to tag.
  override def markDirty() {
    if (!container.hasTag) {
      container.setTag(new CompoundTag())
    }
    save(container.getTag)
  }
}

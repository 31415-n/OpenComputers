package li.cil.oc.common.container

import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.common.entity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

class Drone(playerInventory: Inventory, val drone: entity.Drone) extends Player(playerInventory, drone.mainInventory) {
  val deltaY = 0

  for (i <- 0 to 1) {
    val y = 8 + i * slotSize - deltaY
    for (j <- 0 to 3) {
      val x = 98 + j * slotSize
      addSlotToContainer(new InventorySlot(this, otherInventory, inventorySlots.size, x, y))
    }
  }

  addPlayerInventorySlots(8, 66)

  class InventorySlot(container: Player, inventory: Container, index: Int, x: Int, y: Int) extends StaticComponentSlot(container, inventory, index, x, y, common.Slot.Any, common.Tier.Any) {
    def isValid = (0 until drone.mainInventory.getContainerSize).contains(getSlotIndex)

    @OnlyIn(Dist.CLIENT) override
    def isEnabled = isValid && super.isEnabled

    override def getBackgroundLocation =
      if (isValid) super.getBackgroundLocation
      else Textures.Icons.get(common.Tier.None)

    override def getItem = {
      if (isValid) super.getItem
      else ItemStack.EMPTY
    }
  }

}

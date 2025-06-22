package li.cil.oc.common.container

import li.cil.oc.common.Slot
import li.cil.oc.common.tileentity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.nbt.CompoundTag

class Printer(playerInventory: Inventory, val printer: tileentity.Printer) extends Player(playerInventory, printer) {
  addSlotToContainer(18, 19, Slot.Filtered)
  addSlotToContainer(18, 51, Slot.Filtered)
  addSlotToContainer(152, 35)

  // Show the player's inventory.
  addPlayerInventorySlots(8, 84)

  def progress = synchronizedData.getDouble("progress")

  def amountMaterial = synchronizedData.getInt("amountMaterial")

  def amountInk = synchronizedData.getInt("amountInk")

  override protected def detectCustomDataChanges(nbt: CompoundTag): Unit = {
    synchronizedData.setDouble("progress", if (printer.isPrinting) printer.progress / 100.0 else 0)
    synchronizedData.setInt("amountMaterial", printer.amountMaterial)
    synchronizedData.setInt("amountInk", printer.amountInk)
    super.detectCustomDataChanges(nbt)
  }
}

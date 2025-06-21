package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.Container

class DiskDrive(playerInventory: Inventory, val drive: Container) extends DynamicGuiContainer(new container.DiskDrive(playerInventory, drive)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    fontRenderer.drawString(
      Localization.localizeImmediately(drive.getName),
      8, 6, 0x404040)
  }
}

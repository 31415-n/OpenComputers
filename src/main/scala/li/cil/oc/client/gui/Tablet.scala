package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.item.TabletWrapper
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AbstractContainerMenu

class Tablet(playerInventory: Inventory, val tablet: TabletWrapper) extends DynamicGuiContainer(new container.Tablet(playerInventory, tablet).asInstanceOf[AbstractContainerMenu]) with traits.LockedHotbar {
  override def lockedStack = tablet.stack

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int): Unit = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    guiGraphics.drawString(
      font,
      Localization.localizeImmediately(tablet.getDisplayName.getString),
      8, 6, 0x404040)
  }
}

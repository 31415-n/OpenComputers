package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.world.entity.player.Inventory

class Charger(playerInventory: Inventory, val charger: tileentity.Charger) extends DynamicGuiContainer(new container.Charger(playerInventory, charger)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int) = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    guiGraphics.drawString(font,
      Localization.localizeImmediately(charger.getDisplayName.getString),
      8, 6, 0x404040)
  }
}

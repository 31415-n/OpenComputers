package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.client.gui.GuiGraphics

class Adapter(playerInventory: Inventory, val adapter: tileentity.Adapter) extends DynamicGuiContainer(new container.Adapter(playerInventory, adapter)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int): Unit = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    // Get adapter display name - using a fallback since getName might not exist
    val displayName = try {
      Localization.localizeImmediately("tile.oc.adapter.name")
    } catch {
      case _: Exception => "Adapter"
    }
    // guiGraphics is available from the parent class
    if (guiGraphics != null) {
      guiGraphics.drawString(font, displayName, 8, 6, 0x404040)
    }
  }
}

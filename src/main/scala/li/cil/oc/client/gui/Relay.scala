package li.cil.oc.client.gui

import java.text.DecimalFormat

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import net.minecraft.client.Minecraft
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.world.entity.player.Inventory
import net.minecraft.client.gui.GuiGraphics

class Relay(playerInventory: Inventory, val relay: tileentity.Relay) extends DynamicGuiContainer(new container.Relay(playerInventory, relay)) {
  private val format = new DecimalFormat("#.##hz")

  val tabPosition = (imageWidth, 10, 23, 26) // (x, y, width, height)

  override protected def drawSecondaryBackgroundLayer(): Unit = {
    super.drawSecondaryBackgroundLayer()

    // Tab background.
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    RenderSystem.setShaderTexture(0, Textures.GUI.UpgradeTab)
    val (tabX, tabY, tabW, tabH) = tabPosition
    val x = windowX + tabX
    val y = windowY + tabY
    val t = Tesselator.getInstance
    val r = t.getBuilder
    r.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
    r.vertex(x, y + tabH, 0).uv(0, 1).endVertex()
    r.vertex(x + tabW, y + tabH, 0).uv(1, 1).endVertex()
    r.vertex(x + tabW, y, 0).uv(1, 0).endVertex()
    r.vertex(x, y, 0).uv(0, 0).endVertex()
    t.end()
  }

  override def mouseClicked(mouseX: Int, mouseY: Int, button: Int): Unit = {
    // So MC doesn't throw away the item in the upgrade slot when we're trying to pick it up...
    val originalWidth = imageWidth
    try {
      imageWidth += tabPosition._3 // width
      super.mouseClicked(mouseX, mouseY, button)
    }
    finally {
      imageWidth = originalWidth
    }
  }

  override def mouseReleased(mouseX: Int, mouseY: Int, button: Int): Unit = {
    // So MC doesn't throw away the item in the upgrade slot when we're trying to pick it up...
    val originalWidth = imageWidth
    try {
      imageWidth += tabPosition._3 // width
      super.mouseReleased(mouseX, mouseY, button)
    }
    finally {
      imageWidth = originalWidth
    }
  }

  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int): Unit = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    guiGraphics.drawString(font,
      Localization.localizeImmediately(relay.getDisplayName.getString),
      8, 6, 0x404040)

    guiGraphics.drawString(font,
      Localization.Switch.TransferRate,
      14, 20, 0x404040)
    guiGraphics.drawString(font,
      Localization.Switch.PacketsPerCycle,
      14, 39, 0x404040)
    guiGraphics.drawString(font,
      Localization.Switch.QueueSize,
      14, 58, 0x404040)

    val relayContainer = menu.asInstanceOf[container.Relay]
    guiGraphics.drawString(font,
      format.format(20f / relayContainer.relayDelay),
      108, 20, 0x404040)
    guiGraphics.drawString(font,
      relayContainer.packetsPerCycleAvg + " / " + relayContainer.relayAmount,
      108, 39, thresholdBasedColor(relayContainer.packetsPerCycleAvg, math.ceil(relayContainer.relayAmount / 2f).toInt, relayContainer.relayAmount))
    guiGraphics.drawString(font,
      relayContainer.queueSize + " / " + relayContainer.maxQueueSize,
      108, 58, thresholdBasedColor(relayContainer.queueSize, relayContainer.maxQueueSize / 2, relayContainer.maxQueueSize))
  }

  private def thresholdBasedColor(value: Int, yellow: Int, red: Int) = {
    if (value < yellow) 0x009900
    else if (value < red) 0x999900
    else 0x990000
  }
}

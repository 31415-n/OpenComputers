package li.cil.oc.client.gui

import li.cil.oc.Localization
import li.cil.oc.client.Textures
import li.cil.oc.common.container
import li.cil.oc.common.tileentity
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import scala.jdk.CollectionConverters._
import net.minecraft.world.entity.player.Inventory

class Raid(playerInventory: Inventory, val raid: tileentity.Raid) extends DynamicGuiContainer(new container.Raid(playerInventory, raid)) {
  override def drawSecondaryForegroundLayer(mouseX: Int, mouseY: Int): Unit = {
    super.drawSecondaryForegroundLayer(mouseX, mouseY)
    guiGraphics.drawString(font,
      Localization.localizeImmediately(raid.getDisplayName.getString),
      8, 6, 0x404040)

    // Split string rendering needs to be handled differently in 1.20.1
    val lines = font.split(net.minecraft.network.chat.Component.literal(Localization.Raid.Warning), width - 16)
    for ((line, i) <- lines.asScala.zipWithIndex) {
      guiGraphics.drawString(font, line, 8, 46 + i * font.lineHeight, 0x404040)
    }
  }

  override def renderBg(guiGraphics: GuiGraphics, dt: Float, mouseX: Int, mouseY: Int): Unit = {
    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f) // Required under Linux.
    RenderSystem.setShaderTexture(0, Textures.GUI.Raid)
    guiGraphics.blit(Textures.GUI.Raid, leftPos, topPos, 0, 0, imageWidth, imageHeight)
  }
}

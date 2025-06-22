package li.cil.oc.client.gui

import java.util

import li.cil.oc.client.gui.widget.WidgetContainer
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.world.inventory.AbstractContainerMenu

import scala.jdk.CollectionConverters._

// Workaround because certain other mods *cough*TMI*cough* do base class
// transformations that break things! Such fun. Many annoyed. And yes, this
// is a common issue, have a look at EnderIO and Enchanting Plus. They have
// to work around this, too.
abstract class CustomGuiContainer(val inventoryContainer: AbstractContainerMenu) extends AbstractContainerScreen[AbstractContainerMenu](inventoryContainer, new net.minecraft.world.entity.player.Inventory(null), net.minecraft.network.chat.Component.empty()) with WidgetContainer {
  override def windowX = leftPos

  override def windowY = topPos

  override def windowZ = 0

  override def doesGuiPauseGame = false

  protected def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  // Pretty much Scalaified copy-pasta from base-class.
  def drawHoveringText(guiGraphics: net.minecraft.client.gui.GuiGraphics, text: util.List[String], x: Int, y: Int, font: Font): Unit = {
    copiedDrawHoveringText(guiGraphics, text, x, y, font)
  }

  protected def copiedDrawHoveringText(guiGraphics: net.minecraft.client.gui.GuiGraphics, text: util.List[String], x: Int, y: Int, font: Font): Unit = {
    if (!text.isEmpty) {
      RenderSystem.disableDepthTest()

      val textWidth = text.asScala.map(line => font.width(line)).max

      var posX = x + 12
      var posY = y - 12
      var textHeight = 8
      if (text.size > 1) {
        textHeight += 2 + (text.size - 1) * 10
      }
      if (posX + textWidth > width) {
        posX -= 28 + textWidth
      }
      if (posY + textHeight + 6 > height) {
        posY = height - textHeight - 6
      }

      // Z-level handling changed in 1.20.1
      val bg = 0xF0100010
      guiGraphics.fillGradient(posX - 3, posY - 4, posX + textWidth + 3, posY - 3, bg, bg)
      guiGraphics.fillGradient(posX - 3, posY + textHeight + 3, posX + textWidth + 3, posY + textHeight + 4, bg, bg)
      guiGraphics.fillGradient(posX - 3, posY - 3, posX + textWidth + 3, posY + textHeight + 3, bg, bg)
      guiGraphics.fillGradient(posX - 4, posY - 3, posX - 3, posY + textHeight + 3, bg, bg)
      guiGraphics.fillGradient(posX + textWidth + 3, posY - 3, posX + textWidth + 4, posY + textHeight + 3, bg, bg)
      val color1 = 0x505000FF
      val color2 = (color1 & 0x00FEFEFE) >> 1 | (color1 & 0xFF000000)
      guiGraphics.fillGradient(posX - 3, posY - 3 + 1, posX - 3 + 1, posY + textHeight + 3 - 1, color1, color2)
      guiGraphics.fillGradient(posX + textWidth + 2, posY - 3 + 1, posX + textWidth + 3, posY + textHeight + 3 - 1, color1, color2)
      guiGraphics.fillGradient(posX - 3, posY - 3, posX + textWidth + 3, posY - 3 + 1, color1, color1)
      guiGraphics.fillGradient(posX - 3, posY + textHeight + 2, posX + textWidth + 3, posY + textHeight + 3, color2, color2)

      for ((line, index) <- text.asScala.zipWithIndex) {
        guiGraphics.drawString(font, line.asInstanceOf[String], posX, posY, -1)
        if (index == 0) {
          posY += 2
        }
        posY += 10
      }
      RenderSystem.enableDepthTest()
    }
  }



  override def render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    this.renderBackground(guiGraphics)
    super.render(guiGraphics, mouseX, mouseY, partialTicks)
    this.renderTooltip(guiGraphics, mouseX, mouseY)
  }
}

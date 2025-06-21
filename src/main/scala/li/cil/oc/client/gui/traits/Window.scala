package li.cil.oc.client.gui.traits

import java.util

import li.cil.oc.util.OldScaledResolution
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

trait Window extends Screen {
  var guiLeft = 0
  var guiTop = 0
  var xSize = 0
  var ySize = 0

  val windowWidth = 176
  val windowHeight = 166

  def backgroundImage: ResourceLocation

  protected def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])

  override def isPauseScreen(): Boolean = false

  override def init(): Unit = {
    super.init()

    val guiSize = new OldScaledResolution(minecraft, windowWidth, windowHeight)
    val (midX, midY) = (width / 2, height / 2)
    guiLeft = midX - guiSize.getScaledWidth / 2
    guiTop = midY - guiSize.getScaledHeight / 2
    xSize = guiSize.getScaledWidth
    ySize = guiSize.getScaledHeight
  }

  override def render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float): Unit = {
    guiGraphics.blit(backgroundImage, guiLeft, guiTop, 0, 0, xSize, ySize, windowWidth, windowHeight)

    super.render(guiGraphics, mouseX, mouseY, partialTick)
  }

}

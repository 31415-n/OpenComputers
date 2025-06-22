package li.cil.oc.client.gui.widget

import li.cil.oc.client.Textures
import net.minecraft.client.gui.GuiGraphics
import com.mojang.blaze3d.systems.RenderSystem

class ProgressBar(val x: Int, val y: Int) extends Widget {
  override def width = 140

  override def height = 12

  def barTexture = Textures.GUI.Bar

  var level = 0.0

  override def render(guiGraphics: GuiGraphics): Unit = {
    if (level > 0) {
      val tx = owner.windowX + x
      val ty = owner.windowY + y
      val w = (width * level).toInt
      
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
      
      // Draw progress bar using blit with UV coordinates
      // The progress bar texture uses UV mapping where u goes from 0 to level
      val u0 = 0.0f
      val u1 = level.toFloat
      val v0 = 0.0f
      val v1 = 1.0f
      
      // Use blit with UV coordinates for proper texture mapping
      guiGraphics.blit(barTexture, tx, ty, 0, 0, w, height)
    }
  }
}
package li.cil.oc.client.gui

import li.cil.oc.client.Textures
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.GuiGraphics
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraft.network.chat.Component
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

@OnlyIn(Dist.CLIENT)
class ImageButton(xPos: Int, yPos: Int, w: Int, h: Int,
                  val image: ResourceLocation = null,
                  text: String = null,
                  val canToggle: Boolean = false,
                  val textColor: Int = 0xE0E0E0,
                  val textDisabledColor: Int = 0xA0A0A0,
                  val textHoverColor: Int = 0xFFFFA0,
                  val textIndent: Int = -1,
                  onPress: Button.OnPress = _ => {}) extends Button(xPos, yPos, w, h, 
                    if (text != null) Component.literal(text) else Component.empty(), onPress, Button.DEFAULT_NARRATION) {

  var toggled = false

  var hoverOverride = false

  override def renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    if (visible) {
      val mc = Minecraft.getInstance()
      RenderSystem.setShader(() => GameRenderer.getPositionTexShader())
      RenderSystem.setShaderTexture(0, image)
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
      
      val isHovered = hoverOverride || isHoveredOrFocused()

      if (image != null) {
        val u0 = if (toggled) 0.5f else 0.0f
        val u1 = u0 + (if (canToggle) 0.5f else 1.0f)
        val v0 = if (isHovered) 0.5f else 0.0f
        val v1 = v0 + 0.5f

        guiGraphics.blit(image, getX, getY, u0 * 256, v0 * 256, width, height, 256, 256)
      }
      else {
        val alpha = if (isHovered) 0.8f else 0.4f
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha)
        guiGraphics.fill(getX, getY, getX + width, getY + height, 0xFFFFFFFF)
      }

      val message = getMessage
      if (!message.getString.isEmpty) {
        val color =
          if (!active) textDisabledColor
          else if (hoverOverride || isHovered) textHoverColor
          else textColor
        if (textIndent >= 0) {
          guiGraphics.drawString(mc.font, message, textIndent + getX, getY + (height - 8) / 2, color)
        } else {
          guiGraphics.drawCenteredString(mc.font, message, getX + width / 2, getY + (height - 8) / 2, color)
        }
      }
    }
  }
  
  // Helper method for compatibility
  def isMouseOver(mouseX: Int, mouseY: Int): Boolean = {
    mouseX >= getX && mouseY >= getY && mouseX < getX + width && mouseY < getY + height
  }
  
  // Compatibility property
  def enabled: Boolean = active
  def enabled_=(value: Boolean): Unit = active = value
}

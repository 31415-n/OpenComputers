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
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat

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
                    if (text != null) Component.literal(text) else Component.empty(), onPress, 
                    new Button.CreateNarration {
                      override def createNarrationMessage(button: Button): net.minecraft.network.chat.MutableComponent = Component.empty()
                    }) {

  var toggled = false

  var hoverOverride = false

  override def renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float): Unit = {
    if (visible) {
      val mc = Minecraft.getInstance()
      Textures.bind(image)
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
      
      val x0 = getX
      val x1 = getX + width
      val y0 = getY
      val y1 = getY + height
      
      val isHovered = hoverOverride || isHoveredOrFocused()

      val t = Tesselator.getInstance
      val r = t.getBuilder
      if (image != null) {
        val u0 = if (toggled) 0.5 else 0
        val u1 = u0 + (if (canToggle) 0.5 else 1)
        val v0 = if (isHovered) 0.5 else 0
        val v1 = v0 + 0.5

        r.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        r.vertex(x0, y1, 0).uv(u0.toFloat, v1.toFloat).endVertex()
        r.vertex(x1, y1, 0).uv(u1.toFloat, v1.toFloat).endVertex()
        r.vertex(x1, y0, 0).uv(u1.toFloat, v0.toFloat).endVertex()
        r.vertex(x0, y0, 0).uv(u0.toFloat, v0.toFloat).endVertex()
      }
      else if (isHovered) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.8f)
        r.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
        r.vertex(x0, y1, 0).endVertex()
        r.vertex(x1, y1, 0).endVertex()
        r.vertex(x1, y0, 0).endVertex()
        r.vertex(x0, y0, 0).endVertex()
      }
      else {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.4f)
        r.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
        r.vertex(x0, y1, 0).endVertex()
        r.vertex(x1, y1, 0).endVertex()
        r.vertex(x1, y0, 0).endVertex()
        r.vertex(x0, y0, 0).endVertex()
      }
      t.end()

      val message = getMessage
      if (message != null && !message.getString.isEmpty) {
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
  
  // Compatibility properties
  def enabled: Boolean = active
  def enabled_=(value: Boolean): Unit = active = value
  
  def displayString: String = getMessage.getString
  def displayString_=(value: String): Unit = setMessage(Component.literal(value))
}

package li.cil.oc.client.gui

import li.cil.oc.api
import li.cil.oc.client.renderer.TextBufferRenderCache
import li.cil.oc.client.renderer.gui.BufferRenderer
import li.cil.oc.util.RenderState
import com.mojang.blaze3d.platform.GlStateManager
import net.minecraft.network.chat.Component

class Screen(val buffer: api.internal.TextBuffer, val hasMouse: Boolean, val hasKeyboardCallback: () => Boolean, val hasPower: () => Boolean) extends net.minecraft.client.gui.screens.Screen(Component.literal("OpenComputers Screen")) with traits.InputBuffer {
  override protected def hasKeyboard = hasKeyboardCallback()

  override protected def bufferX = 8 + x

  override protected def bufferY = 8 + y

  private val bufferMargin = BufferRenderer.margin + BufferRenderer.innerMargin

  private var didClick = false

  private var x, y = 0

  private var mx, my = -1

  override def mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean = {
    if (hasMouse && delta != 0) {
      toBufferCoordinates(mouseX.toInt, mouseY.toInt) match {
        case Some((bx, by)) =>
          val scroll = math.signum(delta)
          buffer.mouseScroll(bx, by, scroll, null)
          true
        case _ => super.mouseScrolled(mouseX, mouseY, delta)
      }
    } else {
      super.mouseScrolled(mouseX, mouseY, delta)
    }
  }

  override def mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    if (hasMouse) {
      if (button == 0 || button == 1) {
        clickOrDrag(mouseX.toInt, mouseY.toInt, button)
        true
      } else {
        super.mouseClicked(mouseX, mouseY, button)
      }
    } else {
      super.mouseClicked(mouseX, mouseY, button)
    }
  }

  override def mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean = {
    if (hasMouse) {
      if (button == 0 || button == 1) {
        clickOrDrag(mouseX.toInt, mouseY.toInt, button)
        true
      } else {
        super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
      }
    } else {
      super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }
  }

  override def mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean = {
    if (hasMouse && button >= 0) {
      if (didClick) {
        toBufferCoordinates(mouseX.toInt, mouseY.toInt) match {
          case Some((bx, by)) => buffer.mouseUp(bx, by, button, null)
          case _ => buffer.mouseUp(-1.0, -1.0, button, null)
        }
      }
      didClick = false
      mx = -1
      my = -1
      true
    } else {
      super.mouseReleased(mouseX, mouseY, button)
    }
  }

  private def clickOrDrag(mouseX: Int, mouseY: Int, button: Int) {
    toBufferCoordinates(mouseX, mouseY) match {
      case Some((bx, by)) if bx.toInt != mx || (by*2).toInt != my =>
        if (mx >= 0 && my >= 0) buffer.mouseDrag(bx, by, button, null)
        else buffer.mouseDown(bx, by, button, null)
        didClick = true
        mx = bx.toInt
        my = (by*2).toInt // for high precision mode, sends some unnecessary packets when not using it, but eh
      case _ =>
    }
  }

  private def toBufferCoordinates(mouseX: Int, mouseY: Int): Option[(Double, Double)] = {
    val bx = (mouseX - x - bufferMargin) / scale / TextBufferRenderCache.renderer.charRenderWidth
    val by = (mouseY - y - bufferMargin) / scale / TextBufferRenderCache.renderer.charRenderHeight
    val bw = buffer.getViewportWidth
    val bh = buffer.getViewportHeight
    if (bx >= 0 && by >= 0 && bx < bw && by < bh) Some((bx, by))
    else None
  }

  override def render(guiGraphics: net.minecraft.client.gui.GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float): Unit = {
    super.render(guiGraphics, mouseX, mouseY, partialTick)
    drawBufferLayer()
  }

  override def drawBuffer() {
    com.mojang.blaze3d.systems.RenderSystem.getModelViewStack.pushPose()
    com.mojang.blaze3d.systems.RenderSystem.getModelViewStack.translate(x, y, 0)
    com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix()
    BufferRenderer.drawBackground()
    if (hasPower()) {
      com.mojang.blaze3d.systems.RenderSystem.getModelViewStack.translate(bufferMargin, bufferMargin, 0)
      com.mojang.blaze3d.systems.RenderSystem.getModelViewStack.scale(scale.toFloat, scale.toFloat, 1.0f)
      com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix()
      RenderState.makeItBlend()
      BufferRenderer.drawText(buffer)
    }
  }

  override protected def changeSize(w: Double, h: Double, recompile: Boolean) = {
    val bw = buffer.renderWidth
    val bh = buffer.renderHeight
    val scaleX = math.min(width / (bw + bufferMargin * 2.0), 1)
    val scaleY = math.min(height / (bh + bufferMargin * 2.0), 1)
    val scale = math.min(scaleX, scaleY)
    val innerWidth = (bw * scale).toInt
    val innerHeight = (bh * scale).toInt
    x = (width - (innerWidth + bufferMargin * 2)) / 2
    y = (height - (innerHeight + bufferMargin * 2)) / 2
    if (recompile) {
      BufferRenderer.compileBackground(innerWidth, innerHeight)
    }
    scale
  }
}

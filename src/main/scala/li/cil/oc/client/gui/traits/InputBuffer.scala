package li.cil.oc.client.gui.traits

import li.cil.oc.api
import li.cil.oc.client.KeyBindings
import li.cil.oc.client.Textures
import li.cil.oc.integration.util.ItemSearch
import li.cil.oc.util.RenderState
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11

import scala.collection.mutable

trait InputBuffer extends DisplayBuffer {
  protected def buffer: api.internal.TextBuffer

  override protected def bufferColumns = if (buffer == null) 0 else buffer.getViewportWidth

  override protected def bufferRows = if (buffer == null) 0 else buffer.getViewportHeight

  protected def hasKeyboard: Boolean

  private val pressedKeys = mutable.Map.empty[Int, Char]

  private var showKeyboardMissing = 0L

  override def doesGuiPauseGame = false

  override def init(): Unit = {
    super.init()
    // Keyboard repeat events are handled differently in 1.20.1
  }

  override protected def drawBufferLayer() {
    super.drawBufferLayer()

    if (System.currentTimeMillis() - showKeyboardMissing < 1000) {
      Textures.bind(Textures.GUI.KeyboardMissing)
      GlStateManager._disableDepthTest()

      val x = bufferX + buffer.renderWidth - 16
      val y = bufferY + buffer.renderHeight - 16

      val t = Tesselator.getInstance
      val r = t.getBuilder
      r.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
      r.vertex(x, y + 16, 0).uv(0, 1).endVertex()
      r.vertex(x + 16, y + 16, 0).uv(1, 1).endVertex()
      r.vertex(x + 16, y, 0).uv(1, 0).endVertex()
      r.vertex(x, y, 0).uv(0, 0).endVertex()
      t.end()

      GlStateManager._enableDepthTest()

      RenderState.checkError(getClass.getName + ".drawBufferLayer: keyboard icon")
    }
  }

  override def onClose(): Unit = {
    super.onClose()
    if (buffer != null) for ((code, char) <- pressedKeys) {
      buffer.keyUp(char, code, null)
    }
    // Keyboard repeat events cleanup handled differently in 1.20.1
  }

  override def keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = {
    // Skip if ItemSearch is focused (equivalent to original GuiContainer check)
    if (this.isInstanceOf[AbstractContainerScreen[_]] && ItemSearch.isInputFocused) {
      return super.keyPressed(keyCode, scanCode, modifiers)
    }

    // Handle buffer input (equivalent to original handleKeyboardInput logic)
    if (buffer != null && keyCode != GLFW.GLFW_KEY_ESCAPE && keyCode != GLFW.GLFW_KEY_F11) {
      if (hasKeyboard) {
        // Convert keyCode to character using proper GLFW mapping
        val char = convertKeyCodeToChar(keyCode, scanCode, modifiers)
        
        // Check if this is a new key press or should ignore repeat
        if (!pressedKeys.contains(keyCode) || !ignoreRepeat(char, keyCode)) {
          buffer.keyDown(char, keyCode, null)
          pressedKeys += keyCode -> char
        }

        // Handle clipboard paste (equivalent to original KeyBindings.isPastingClipboard check)
        if (KeyBindings.isPastingClipboard) {
          buffer.clipboard(minecraft.keyboardHandler.getClipboard, null)
        }
        true
      }
      else {
        showKeyboardMissing = System.currentTimeMillis()
        false
      }
    } else {
      super.keyPressed(keyCode, scanCode, modifiers)
    }
  }

  override def keyReleased(keyCode: Int, scanCode: Int, modifiers: Int): Boolean = {
    // Handle key release for buffer (equivalent to original Keyboard.getEventKeyState == false)
    if (buffer != null) {
      pressedKeys.remove(keyCode) match {
        case Some(char) => buffer.keyUp(char, keyCode, null)
        case _ => // Wasn't pressed while viewing the screen.
      }
    }
    super.keyReleased(keyCode, scanCode, modifiers)
  }

  override def charTyped(codePoint: Char, modifiers: Int): Boolean = {
    // Handle character typing for proper text input (this provides the actual character)
    if (buffer != null && hasKeyboard) {
      // This gives us the actual typed character, which is more accurate than key code conversion
      val keyCode = codePoint.toInt
      if (!pressedKeys.contains(keyCode)) {
        buffer.keyDown(codePoint, keyCode, null)
        pressedKeys += keyCode -> codePoint
      }
      true
    } else {
      super.charTyped(codePoint, modifiers)
    }
  }

  /**
   * Convert GLFW key code to character, handling special cases and modifiers.
   * This replaces the original Keyboard.getEventCharacter functionality.
   */
  private def convertKeyCodeToChar(keyCode: Int, scanCode: Int, modifiers: Int): Char = {
    // Handle special keys that don't produce characters
    keyCode match {
      case GLFW.GLFW_KEY_ENTER => '\r'
      case GLFW.GLFW_KEY_TAB => '\t'
      case GLFW.GLFW_KEY_BACKSPACE => '\b'
      case GLFW.GLFW_KEY_DELETE => 127.toChar
      case GLFW.GLFW_KEY_ESCAPE => 27.toChar
      case _ if keyCode >= GLFW.GLFW_KEY_SPACE && keyCode <= GLFW.GLFW_KEY_GRAVE_ACCENT =>
        // Printable ASCII characters
        val baseChar = keyCode.toChar
        // Handle shift modifier for uppercase
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
          baseChar.toUpper
        } else {
          baseChar.toLower
        }
      case _ => 0.toChar // Non-printable or special key
    }
  }

  override def mouseClicked(x: Double, y: Double, button: Int): Boolean = {
    val result = super.mouseClicked(x, y, button)
    val isMiddleMouseButton = button == 2
    val isBoundMouseButton = KeyBindings.isPastingClipboard
    if (buffer != null && (isMiddleMouseButton || isBoundMouseButton)) {
      if (hasKeyboard) {
        buffer.clipboard(minecraft.keyboardHandler.getClipboard, null)
      }
      else {
        showKeyboardMissing = System.currentTimeMillis()
      }
    }
    result
  }

  /**
   * Check if key repeat should be ignored for modifier keys.
   * Equivalent to original ignoreRepeat function with LWJGL2 key codes.
   */
  private def ignoreRepeat(char: Char, code: Int): Boolean = {
    code == GLFW.GLFW_KEY_LEFT_CONTROL ||
      code == GLFW.GLFW_KEY_RIGHT_CONTROL ||
      code == GLFW.GLFW_KEY_LEFT_ALT ||
      code == GLFW.GLFW_KEY_RIGHT_ALT ||
      code == GLFW.GLFW_KEY_LEFT_SHIFT ||
      code == GLFW.GLFW_KEY_RIGHT_SHIFT ||
      code == GLFW.GLFW_KEY_LEFT_SUPER ||
      code == GLFW.GLFW_KEY_RIGHT_SUPER
  }
}

package li.cil.oc.client

import li.cil.oc.OpenComputers
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import com.mojang.blaze3d.platform.InputConstants
import org.lwjgl.glfw.GLFW

import scala.collection.mutable

/**
 * Key binding management for OpenComputers client-side functionality.
 * Handles extended tooltips and clipboard operations with full 1.20.1 compatibility.
 */
object KeyBindings {
  /** Collection of key binding check functions for extensibility */
  val keyBindingChecks = mutable.ArrayBuffer(isKeyBindingPressedVanilla _)

  /** Collection of key binding name getter functions for extensibility */
  val keyBindingNameGetters = mutable.ArrayBuffer(getKeyBindingNameVanilla _)

  /** Check if extended tooltips should be shown (when sneak key is pressed) */
  def showExtendedTooltips: Boolean = isKeyBindingPressed(extendedTooltip)

  /** Check if clipboard paste operation is active */
  def isPastingClipboard: Boolean = isKeyBindingPressed(clipboardPaste)

  /**
   * Get display name for a key binding using all registered name getters.
   * @param keyBinding The key binding to get name for
   * @return Display name or "???" if none found
   */
  def getKeyBindingName(keyBinding: KeyMapping): String = keyBindingNameGetters.map(_(keyBinding)).collectFirst {
    case Some(name) => name
  }.getOrElse("???")

  /**
   * Check if a key binding is currently pressed using all registered checkers.
   * @param keyBinding The key binding to check
   * @return true if all checkers confirm the key is pressed
   */
  def isKeyBindingPressed(keyBinding: KeyMapping): Boolean = keyBindingChecks.forall(_(keyBinding))

  /**
   * Get vanilla display name for a key binding.
   * @param keyBinding The key binding to get name for
   * @return Some(name) if successful, None if failed
   */
  def getKeyBindingNameVanilla(keyBinding: KeyMapping): Option[String] = try {
    Some(keyBinding.getTranslatedKeyMessage.getString)
  } catch {
    case _: Throwable => None
  }

  /**
   * Check if a key binding is pressed using vanilla Minecraft input system.
   * @param keyBinding The key binding to check
   * @return true if the key is currently pressed
   */
  def isKeyBindingPressedVanilla(keyBinding: KeyMapping): Boolean = try {
    val minecraft = Minecraft.getInstance()
    if (minecraft.screen != null) {
      // Don't process key bindings when GUI is open
      false
    } else {
      val key = keyBinding.getKey
      key.getType match {
        case InputConstants.Type.KEYSYM =>
          // Keyboard key
          val window = minecraft.getWindow.getWindow
          GLFW.glfwGetKey(window, key.getValue) == GLFW.GLFW_PRESS
        case InputConstants.Type.MOUSE =>
          // Mouse button
          val window = minecraft.getWindow.getWindow
          GLFW.glfwGetMouseButton(window, key.getValue) == GLFW.GLFW_PRESS
        case _ => false
      }
    }
  } catch {
    case _: Throwable => false
  }

  /** Extended tooltip key binding (uses vanilla sneak key) */
  def extendedTooltip: KeyMapping = Minecraft.getInstance().options.keyShift

  /** Clipboard paste key binding (Insert key) */
  val clipboardPaste = new KeyMapping(
    "key.clipboardPaste",
    InputConstants.Type.KEYSYM,
    GLFW.GLFW_KEY_INSERT,
    OpenComputers.Name
  )
}

package li.cil.oc.integration.rei

import net.minecraft.world.item.ItemStack
import scala.jdk.CollectionConverters._

/**
 * REI (Roughly Enough Items) integration for OpenComputers.
 * Provides item highlighting and filtering functionality similar to JEI.
 */
object ModREI {
  
  /**
   * Highlights the specified item stacks in REI overlay.
   * @param stacks Collection of ItemStack to highlight
   */
  def highlightStacks(stacks: java.util.Collection[ItemStack]): Unit = {
    try {
      // REI highlighting implementation will be added when REI dependency is available
      // For now, this is a no-op to allow compilation
    } catch {
      case _: Exception => // Silently ignore REI integration errors
    }
  }
  
  /**
   * Clears all highlighted stacks in REI overlay.
   */
  def clearHighlights(): Unit = {
    try {
      highlightStacks(java.util.Collections.emptyList())
    } catch {
      case _: Exception => // Silently ignore REI integration errors
    }
  }
  
  /**
   * Checks if REI is available and loaded.
   * @return true if REI is available, false otherwise
   */
  def isAvailable: Boolean = {
    try {
      // Check if REI classes are available
      Class.forName("me.shedaniel.rei.api.client.registry.display.DisplayRegistry")
      true
    } catch {
      case _: ClassNotFoundException => false
    }
  }
}
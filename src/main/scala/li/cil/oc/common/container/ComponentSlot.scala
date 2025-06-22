package li.cil.oc.common.container

import li.cil.oc.common
import net.minecraft.world.entity.player.{Player => EntityPlayer}
import net.minecraft.world.Container
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.jdk.CollectionConverters._

abstract class ComponentSlot(inventory: Container, index: Int, x: Int, y: Int) extends Slot(inventory, index, x, y) {
  def container: li.cil.oc.common.container.Player

  def slot: String

  def tier: Int

  def tierIcon: ResourceLocation

  var changeListener: Option[Slot => Unit] = None

  // ----------------------------------------------------------------------- //

  def hasBackground = getBackgroundLocation != null

  @OnlyIn(Dist.CLIENT)
  def isEnabled = slot != common.Slot.None && tier != common.Tier.None && isActive

  override def mayPlace(stack: ItemStack): Boolean = container.canPlaceItem(getSlotIndex, stack)

  override def onTake(player: EntityPlayer, stack: ItemStack): Unit = {
    for (slot <- container.slots.asScala) slot match {
      case dynamic: ComponentSlot => dynamic.clearIfInvalid(player)
      case _ =>
    }
    super.onTake(player, stack)
  }

  override def setByPlayer(stack: ItemStack): Unit = {
    super.setByPlayer(stack)
    container match {
      case playerAware: common.tileentity.traits.PlayerInputAware =>
        playerAware.onSetInventorySlotContents(player, getSlotIndex, stack)
      case _ =>
    }
  }

  override def setChanged(): Unit = {
    super.setChanged()
    for (slot <- container.slots.asScala) slot match {
      case dynamic: ComponentSlot => dynamic.clearIfInvalid(player)
      case _ =>
    }
    changeListener.foreach(_(this))
  }

  protected def clearIfInvalid(player: EntityPlayer): Unit = {}
  
  // Helper method to get player from container
  private def player: EntityPlayer = container match {
    case playerContainer: li.cil.oc.common.container.Player => playerContainer.playerInventory.player
    case _ => null // This should not happen in normal cases
  }
}

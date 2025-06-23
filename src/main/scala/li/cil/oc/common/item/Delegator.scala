package li.cil.oc.common.item

import java.util
import li.cil.oc.CreativeTab
import li.cil.oc.OpenComputers
import li.cil.oc.api.driver
import li.cil.oc.api.driver.item.Chargeable
import li.cil.oc.api.event.RobotRenderEvent.MountPoint
import li.cil.oc.api.internal.Robot
import li.cil.oc.client.renderer.item.UpgradeRenderer
import li.cil.oc.common.item.traits.Delegate
import li.cil.oc.integration.opencomputers.{Item => OpenComputersItem}
import li.cil.oc.util.BlockPosition
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.UseAnim
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.InteractionResult
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.core.NonNullList
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object Delegator {
  def subItem(stack: ItemStack): Option[Delegate] =
    if (!stack.isEmpty) stack.getItem match {
      case delegator: Delegator => delegator.subItem(stack.getItemDamage)
      case _ => None
    }
    else None
}

class Delegator extends Item with driver.item.UpgradeRenderer with Chargeable {
  setHasSubtypes(true)
  setCreativeTab(CreativeTab)

  // ----------------------------------------------------------------------- //
  // SubItem
  // ----------------------------------------------------------------------- //

  override def getItemStackLimit(stack: ItemStack): Int =
    Delegator.subItem(stack) match {
      case Some(subItem) => OpenComputersItem.address(stack) match {
        case Some(address) => 1
        case _ => subItem.maxStackSize
      }
      case _ => maxStackSize
    }

  val subItems: ArrayBuffer[Delegate] = mutable.ArrayBuffer.empty[traits.Delegate]

  def add(subItem: traits.Delegate): Int = {
    val itemId = subItems.length
    subItems += subItem
    itemId
  }

  def subItem(damage: Int): Option[Delegate] =
    damage match {
      case itemId if itemId >= 0 && itemId < subItems.length => Some(subItems(itemId))
      case _ => None
    }

  override def fillItemCategory(tab: CreativeModeTab, list: NonNullList[ItemStack]) {
    // Workaround for MC's untyped lists...
    if(allowedIn(tab)){
      subItems.indices.filter(subItems(_).showInItemList).
        map(subItems(_).createItemStack()).
        sortBy(_.getDescriptionId).
        foreach(list.add)
    }
  }

  // ----------------------------------------------------------------------- //
  // Item
  // ----------------------------------------------------------------------- //

  override def getTranslationKey(stack: ItemStack): String =
    Delegator.subItem(stack) match {
      case Some(subItem) => "item.oc." + subItem.unlocalizedName
      case _ => getTranslationKey
    }

  override def isBookEnchantable(itemA: ItemStack, itemB: ItemStack): Boolean = false

  override def getRarity(stack: ItemStack): Rarity =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.rarity(stack)
      case _ => Rarity.COMMON
    }

//  override def getColorFromItemStack(stack: ItemStack, pass: Int) =
//    Delegator.subItem(stack) match {
//      case Some(subItem) => subItem.color(stack, pass)
//      case _ => super.getColorFromItemStack(stack, pass)
//    }

  override def getContainerItem(stack: ItemStack): ItemStack =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.getContainerItem(stack)
      case _ => super.getContainerItem(stack)
    }

  override def hasContainerItem(stack: ItemStack): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.hasContainerItem(stack)
      case _ => super.hasContainerItem(stack)
    }

  // ----------------------------------------------------------------------- //

  override def doesSneakBypassUse(stack: ItemStack, world: BlockGetter, pos: BlockPos, player: Player): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.doesSneakBypassUse(world, pos, player)
      case _ => super.doesSneakBypassUse(stack, world, pos, player)
    }

  override def onItemUseFirst(stack: ItemStack, context: net.minecraft.world.item.context.UseOnContext): InteractionResult =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onItemUseFirst(stack, context.getPlayer, BlockPosition(context.getClickedPos, context.getLevel), context.getClickedFace, context.getClickLocation.x.toFloat, context.getClickLocation.y.toFloat, context.getClickLocation.z.toFloat)
      case _ => super.onItemUseFirst(stack, context)
    }

  override def useOn(context: net.minecraft.world.item.context.UseOnContext): InteractionResult =
    Delegator.subItem(context.getItemInHand) match {
      case Some(subItem) => if (subItem.onItemUse(context.getItemInHand, context.getPlayer, BlockPosition(context.getClickedPos, context.getLevel), context.getClickedFace, context.getClickLocation.x.toFloat, context.getClickLocation.y.toFloat, context.getClickLocation.z.toFloat)) InteractionResult.SUCCESS else InteractionResult.PASS
      case _ => super.useOn(context)
    }

  override def use(world: Level, player: Player, hand: InteractionHand): InteractionResultHolder[ItemStack] =
    player.getItemInHand(hand) match {
      case stack: ItemStack => Delegator.subItem(stack) match {
        case Some(subItem) => subItem.onItemRightClick(stack, world, player)
        case _ => super.use(world, player, hand)
      }
      case _ => super.use(world, player, hand)
    }

  // ----------------------------------------------------------------------- //

  override def finishUsingItem(stack: ItemStack, world: Level, entity: LivingEntity): ItemStack =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onItemUseFinish(stack, world, entity)
      case _ => super.finishUsingItem(stack, world, entity)
    }

  override def getUseAnimation(stack: ItemStack): UseAnim =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.getItemUseAction(stack)
      case _ => super.getUseAnimation(stack)
    }

  override def getUseDuration(stack: ItemStack): Int =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.getMaxItemUseDuration(stack)
      case _ => super.getUseDuration(stack)
    }

  override def releaseUsing(stack: ItemStack, world: Level, entity: LivingEntity, timeLeft: Int): Unit =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.onPlayerStoppedUsing(stack, entity, timeLeft)
      case _ => super.releaseUsing(stack, world, entity, timeLeft)
    }

  def internalGetItemStackDisplayName(stack: ItemStack): String = super.getItemStackDisplayName(stack)

  override def getName(stack: ItemStack): net.minecraft.network.chat.Component =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.displayName(stack) match {
        case Some(name) => net.minecraft.network.chat.Component.literal(name)
        case _ => super.getName(stack)
      }
      case _ => super.getName(stack)
    }

  @OnlyIn(Dist.CLIENT)
  override def appendHoverText(stack: ItemStack, world: Level, tooltip: util.List[net.minecraft.network.chat.Component], flag: TooltipFlag) {
    super.appendHoverText(stack, world, tooltip, flag)
    Delegator.subItem(stack) match {
      case Some(subItem) => try {
        val stringTooltip = new util.ArrayList[String]()
        subItem.tooltipLines(stack, world, stringTooltip, flag)
        import scala.jdk.CollectionConverters._
        stringTooltip.asScala.foreach(line => tooltip.add(net.minecraft.network.chat.Component.literal(line)))
      } catch {
        case t: Throwable => OpenComputers.log.warn("Error in item tooltip.", t)
      }
      case _ => // Nothing to add.
    }
  }

  override def getDamage(stack: ItemStack): Int =
    Delegator.subItem(stack) match {
      case Some(subItem) => (subItem.durability(stack) * getMaxDamage(stack)).toInt
      case _ => super.getDamage(stack)
    }

  override def isBarVisible(stack: ItemStack): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.showDurabilityBar(stack)
      case _ => super.isBarVisible(stack)
    }

  override def inventoryTick(stack: ItemStack, world: Level, player: Entity, slot: Int, selected: Boolean): Unit =
    Delegator.subItem(stack) match {
      case Some(subItem) => subItem.update(stack, world, player, slot, selected)
      case _ => super.inventoryTick(stack, world, player, slot, selected)
    }

  override def toString: String = getTranslationKey

  // ----------------------------------------------------------------------- //

  def canCharge(stack: ItemStack): Boolean =
    Delegator.subItem(stack) match {
      case Some(subItem: Chargeable) => true
      case _ => false
    }

  def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double =
    Delegator.subItem(stack) match {
      case Some(subItem: Chargeable) => subItem.charge(stack, amount, simulate)
      case _ => amount
    }

  // ----------------------------------------------------------------------- //

  override def computePreferredMountPoint(stack: ItemStack, robot: Robot, availableMountPoints: util.Set[String]): String = UpgradeRenderer.preferredMountPoint(stack, availableMountPoints)

  override def render(stack: ItemStack, mountPoint: MountPoint, robot: Robot, pt: Float): Unit = UpgradeRenderer.render(stack, mountPoint)
}

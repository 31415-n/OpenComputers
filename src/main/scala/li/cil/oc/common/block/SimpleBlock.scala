package li.cil.oc.common.block

import java.util

import li.cil.oc.CreativeTab
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.traits.Colored
import li.cil.oc.common.tileentity.traits.Inventory
import li.cil.oc.common.tileentity.traits.Rotatable
import li.cil.oc.util.Color
import li.cil.oc.util.Tooltip
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.material.Material
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Rarity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.InteractionResult
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

abstract class SimpleBlock(material: Material = Material.METAL) extends BaseEntityBlock(Properties.of(material)) {
  import net.minecraft.world.level.block.state.BlockBehaviour.Properties
  setHardness(2f)
  setResistance(5)
  setCreativeTab(CreativeTab)

  var showInItemList = true

  protected val validRotations_ = Array(Direction.UP, Direction.DOWN)

  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = null

  @OnlyIn(Dist.CLIENT)
  override def skipRendering(state: BlockState, adjacentBlockState: BlockState, direction: Direction): Boolean = {
    val bounds = getBoundingBox(state, world, pos)
    (side == EnumFacing.DOWN && bounds.minY > 0) ||
      (side == EnumFacing.UP && bounds.maxY < 1) ||
      (side == EnumFacing.NORTH && bounds.minZ > 0) ||
      (side == EnumFacing.SOUTH && bounds.maxZ < 1) ||
      (side == EnumFacing.WEST && bounds.minX > 0) ||
      (side == EnumFacing.EAST && bounds.maxX < 1) ||
      isOpaqueCube(state)
  }

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  override def getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

  @OnlyIn(Dist.CLIENT)
  def preItemRender(metadata: Int): Unit = {}

  // ----------------------------------------------------------------------- //
  // ItemBlock
  // ----------------------------------------------------------------------- //

  def rarity(stack: ItemStack) = Rarity.COMMON

  @OnlyIn(Dist.CLIENT)
  def addInformation(metadata: Int, stack: ItemStack, world: Level, tooltip: util.List[String], flag: TooltipFlag): Unit = {
    tooltipHead(metadata, stack, world, tooltip, flag)
    tooltipBody(metadata, stack, world, tooltip, flag)
    tooltipTail(metadata, stack, world, tooltip, flag)
  }

  protected def tooltipHead(metadata: Int, stack: ItemStack, world: Level, tooltip: util.List[String], flag: TooltipFlag): Unit = {
  }

  protected def tooltipBody(metadata: Int, stack: ItemStack, world: Level, tooltip: util.List[String], flag: TooltipFlag): Unit = {
    tooltip.addAll(Tooltip.get(getClass.getSimpleName.toLowerCase))
  }

  protected def tooltipTail(metadata: Int, stack: ItemStack, world: Level, tooltip: util.List[String], flag: TooltipFlag): Unit = {
  }

  // ----------------------------------------------------------------------- //
  // Rotation
  // ----------------------------------------------------------------------- //

  def getFacing(world: BlockGetter, pos: BlockPos): Direction =
    world.getBlockEntity(pos) match {
      case tileEntity: Rotatable => tileEntity.facing
      case _ => Direction.SOUTH
    }

  def setFacing(world: Level, pos: BlockPos, value: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.setFromFacing(value); true
      case _ => false
    }

  def setRotationFromEntityPitchAndYaw(world: Level, pos: BlockPos, value: Entity): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.setFromEntityPitchAndYaw(value); true
      case _ => false
    }

  def toLocal(world: BlockGetter, pos: BlockPos, value: Direction): Direction =
    world.getBlockEntity(pos) match {
      case rotatable: Rotatable => rotatable.toLocal(value)
      case _ => value
    }

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  def isBlockSolid(world: BlockGetter, pos: BlockPos, side: Direction): Boolean = world.getBlockState(pos).getMaterial.isSolid

  def canHarvestBlock(world: BlockGetter, pos: BlockPos, player: Player) = true

  def canBeReplacedByLeaves(state: BlockState, world: BlockGetter, pos: BlockPos): Boolean = false

  def canCreatureSpawn(state: BlockState, world: BlockGetter, pos: BlockPos, `type`: MobSpawnType): Boolean = false

  def getValidRotations(world: Level, pos: BlockPos): Array[Direction] = validRotations_

  override def onRemove(state: BlockState, world: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean): Unit = {
    if (!world.isClientSide) world.getBlockEntity(pos) match {
      case inventory: Inventory => inventory.dropAllSlots()
      case _ => // Ignore.
    }
    super.onRemove(state, world, pos, newState, isMoving)
  }

  // ----------------------------------------------------------------------- //

  def rotateBlock(world: Level, pos: BlockPos, axis: Direction): Boolean =
    world.getBlockEntity(pos) match {
      case rotatable: tileentity.traits.Rotatable if rotatable.rotate(axis) =>
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        true
      case _ => false
    }

  def recolorBlock(world: Level, pos: BlockPos, side: Direction, color: DyeColor): Boolean =
    world.getBlockEntity(pos) match {
      case colored: Colored if colored.getColor != Color.rgbValues(color) =>
        colored.setColor(Color.rgbValues(color))
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        true // Blame Vexatos.
      case _ => false
    }

  // ----------------------------------------------------------------------- //

  override def use(state: BlockState, world: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult = {
    val heldItem = player.getItemInHand(hand)
    world.getBlockEntity(pos) match {
      case colored: Colored if Color.isDye(heldItem) =>
        colored.setColor(Color.rgbValues(Color.dyeColor(heldItem)))
        world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        if (!player.getAbilities.instabuild && colored.consumesDye) {
          heldItem.shrink(1)
        }
        InteractionResult.SUCCESS
      case _ => 
        if (localOnBlockActivated(world, pos, player, hand, heldItem, hit.getDirection, hit.getLocation.x.toFloat, hit.getLocation.y.toFloat, hit.getLocation.z.toFloat))
          InteractionResult.SUCCESS
        else
          InteractionResult.PASS
    }
  }

  def localOnBlockActivated(world: Level, pos: BlockPos, player: Player, hand: InteractionHand, heldItem: ItemStack, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = false
}

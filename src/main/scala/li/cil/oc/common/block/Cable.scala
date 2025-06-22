package li.cil.oc.common.block

import java.util

import li.cil.oc.common.block.property.UnlistedInteger
import li.cil.oc.common.capabilities.Capabilities
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.{Entity, LivingEntity}
import net.minecraft.world.item.{DyeColor, ItemStack}
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.phys.{AABB, BlockHitResult, Vec3}
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
// Extended block state system removed in 1.20.1
// import net.minecraftforge.common.property.ExtendedBlockState
// import net.minecraftforge.common.property.IExtendedBlockState

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

class Cable(protected implicit val tileTag: ClassTag[tileentity.Cable]) extends SimpleBlock with traits.CustomDrops[tileentity.Cable] {
  // For Immibis Microblock support.
  val ImmibisMicroblocks_TransformableBlockMarker = null

  // For FMP part coloring.
  var colorMultiplierOverride: Option[Int] = None

  // ----------------------------------------------------------------------- //

  // In 1.20.1, we use block entity data for dynamic properties instead of extended block states
  override def createBlockState() = {
    import net.minecraft.world.level.block.state.StateDefinition
    val builder = new StateDefinition.Builder[Block, BlockState](this)
    // Add any static properties here if needed
    builder.create(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC)
  }

  // Dynamic properties are now handled through block entity data
  def getCableNeighbors(world: BlockGetter, pos: BlockPos): Int = Cable.neighbors(world, pos)
  
  def getCableColor(world: BlockGetter, pos: BlockPos): Int = {
    world.getBlockEntity(pos) match {
      case cable: tileentity.Cable => cable.getColor
      case _ => 0
    }
  }
  
  def getIsSideCable(world: BlockGetter, pos: BlockPos): Int = {
    var isCableMask = 0
    for (side <- Direction.values()) {
      if (world.getBlockEntity(pos.relative(side)).isInstanceOf[tileentity.Cable]) {
        isCableMask = Cable.mask(side, isCableMask)
      }
    }
    isCableMask
  }

  // ----------------------------------------------------------------------- //

  override def isOpaqueCube(state: BlockState): Boolean = false

  override def isFullCube(state: BlockState): Boolean = false

  override def shouldSideBeRendered(state: BlockState, world: BlockGetter, pos: BlockPos, side: Direction) = true

  override def isSideSolid(state: BlockState, world: BlockGetter, pos: BlockPos, side: Direction) = false

  // ----------------------------------------------------------------------- //

  override def getPickBlock(state: BlockState, target: BlockHitResult, world: Level, pos: BlockPos, player: Player) =
    world.getBlockEntity(pos) match {
      case t: tileentity.Cable => t.createItemStack()
      case _ => createItemStack()
    }

  override def getBoundingBox(state: BlockState, world: BlockGetter, pos: BlockPos): AABB = Cable.bounds(world, pos)

  override def addCollisionBoxToList(state: BlockState, worldIn: Level, pos: BlockPos, entityBox: AABB, collidingBoxes: util.List[AABB], entityIn: Entity, isActualState: Boolean): Unit = {
    Cable.parts(worldIn, pos, entityBox, collidingBoxes)
  }

  override def collisionRayTrace(state: BlockState, world: Level, pos: BlockPos, start: Vec3, end: Vec3): BlockHitResult = {
    var distance = Double.PositiveInfinity
    var result: BlockHitResult = null

    val boxes = new util.ArrayList[AABB]
    Cable.parts(world, pos, Block.BLOCK_SUPPORT_SHAPE.bounds().move(pos.getX, pos.getY, pos.getZ), boxes)
    for (part: AABB <- boxes.asScala) {
      val hit = part.clip(start, end)
      if (hit.isPresent) {
        val hitResult = hit.get()
        val hitDistance = hitResult.squareDistanceTo(start)
        if (hitDistance < distance) {
          distance = hitDistance;
          result = new BlockHitResult(hitResult, Direction.getNearest(hitResult.x - pos.getX - 0.5, hitResult.y - pos.getY - 0.5, hitResult.z - pos.getZ - 0.5), pos, false);
        }
      }
    }

    result
  }

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(world: Level, metadata: Int) = new tileentity.Cable()

  // ----------------------------------------------------------------------- //

  override def neighborChanged(state: BlockState, world: Level, pos: BlockPos, neighborBlock: Block, sourcePos: BlockPos) {
    world.sendBlockUpdated(pos, state, state, 3)
    super.neighborChanged(state, world, pos, neighborBlock, sourcePos)
  }

  override protected def doCustomInit(tileEntity: tileentity.Cable, player: LivingEntity, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    if (!tileEntity.getLevel.isClientSide) {
      tileEntity.fromItemStack(stack)
    }
  }

  override protected def doCustomDrops(tileEntity: tileentity.Cable, player: Player, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    if (!player.getAbilities.instabuild) {
      Block.popResource(tileEntity.getLevel, tileEntity.getBlockPos, tileEntity.createItemStack())
    }
  }
}

object Cable {
  final val MIN = 0.375
  final val MAX = 1 - MIN

  final val DefaultBounds: AABB = new AABB(MIN, MIN, MIN, MAX, MAX, MAX)

  final val CachedParts: Array[AABB] = Array(
    new AABB( MIN, 0, MIN, MAX, MIN, MAX ), // Down
    new AABB( MIN, MAX, MIN, MAX, 1, MAX ), // Up
    new AABB( MIN, MIN, 0, MAX, MAX, MIN ), // North
    new AABB( MIN, MIN, MAX, MAX, MAX, 1 ), // South
    new AABB( 0, MIN, MIN, MIN, MAX, MAX ), // West
    new AABB( MAX, MIN, MIN, 1, MAX, MAX )) // East

  final val CachedBounds = {
    // 6 directions = 6 bits = 11111111b >> 2 = 0xFF >> 2
    (0 to 0xFF >> 2).map(mask => {
      Direction.values().foldLeft(DefaultBounds)((bound, side) => {
        if (((1 << side.get3DDataValue()) & mask) != 0) bound.minmax(CachedParts(side.ordinal()))
        else bound
      })
    }).toArray
  }

  final val NeighborsProp = new UnlistedInteger("neighbors")
  final val ColorProp = new UnlistedInteger("color")
  final val IsSideCableProp = new UnlistedInteger("is_cable")

  def mask(side: Direction, value: Int = 0) = value | (1 << side.get3DDataValue())

  def neighbors(world: BlockGetter, pos: BlockPos) = {
    var result = 0
    val tileEntity = world.getBlockEntity(pos)
    for (side <- Direction.values()) {
      val tpos = pos.relative(side)
      val hasNode = hasNetworkNode(tileEntity, side)
      if (hasNode && (world match {
        case world: Level => world.isLoaded(tpos)
        case _ => !world.getBlockState(tpos).isAir
      })) {
        val neighborTileEntity = world.getBlockEntity(tpos)
        if (neighborTileEntity != null && neighborTileEntity.getLevel != null) {
          val neighborHasNode = hasNetworkNode(neighborTileEntity, side.getOpposite)
          val canConnectColor = canConnectBasedOnColor(tileEntity, neighborTileEntity)
          val canConnectIM = canConnectFromSideIM(tileEntity, side) && canConnectFromSideIM(neighborTileEntity, side.getOpposite)
          if (neighborHasNode && canConnectColor && canConnectIM) {
            result = mask(side, result)
          }
        }
      }
    }
    result
  }

  def bounds(world: BlockGetter, pos: BlockPos) = Cable.CachedBounds(Cable.neighbors(world, pos))

  def parts(world: BlockGetter, pos: BlockPos, entityBox : AABB, boxes : util.List[AABB]) = {
    val center = Cable.DefaultBounds.move(pos.getX, pos.getY, pos.getZ)
    if (entityBox.intersects(center)) boxes.add(center)

    val mask = Cable.neighbors(world, pos)
    for (side <- Direction.values()) {
      if(((1 << side.get3DDataValue()) & mask) != 0) {
        val part = Cable.CachedParts(side.ordinal()).move(pos.getX, pos.getY, pos.getZ)
        if (entityBox.intersects(part)) boxes.add(part)
      }
    }
  }

  private def hasNetworkNode(tileEntity: BlockEntity, side: Direction): Boolean = {
    if (tileEntity != null) {
      if (tileEntity.isInstanceOf[tileentity.RobotProxy]) return false

      if (tileEntity.getCapability(Capabilities.SidedEnvironmentCapability, side).isPresent) {
        val host = tileEntity.getCapability(Capabilities.SidedEnvironmentCapability, side).orElse(null)
        if (host != null) {
          return if (tileEntity.getLevel.isClientSide) host.canConnect(side) else host.sidedNode(side) != null
        }
      }

      if (tileEntity.getCapability(Capabilities.EnvironmentCapability, side).isPresent) {
        val host = tileEntity.getCapability(Capabilities.EnvironmentCapability, side).orElse(null)
        if (host != null) return true
      }
    }

    false
  }

  private def getConnectionColor(tileEntity: BlockEntity): Int = {
    if (tileEntity != null) {
      if (tileEntity.getCapability(Capabilities.ColoredCapability, null).isPresent) {
        val colored = tileEntity.getCapability(Capabilities.ColoredCapability, null).orElse(null)
        if (colored != null && colored.controlsConnectivity) return colored.getColor
      }
    }

    Color.rgbValues(DyeColor.LIGHT_GRAY)
  }

  private def canConnectBasedOnColor(te1: BlockEntity, te2: BlockEntity) = {
    val (c1, c2) = (getConnectionColor(te1), getConnectionColor(te2))
    c1 == c2 || c1 == Color.rgbValues(DyeColor.LIGHT_GRAY) || c2 == Color.rgbValues(DyeColor.LIGHT_GRAY)
  }

  private def canConnectFromSideIM(tileEntity: BlockEntity, side: Direction) =
    tileEntity match {
      case im: tileentity.traits.ImmibisMicroblock => im.ImmibisMicroblocks_isSideOpen(side.ordinal)
      case _ => true
    }
}

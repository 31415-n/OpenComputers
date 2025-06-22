package li.cil.oc.common.tileentity

import java.util

import com.google.common.base.Strings
import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity.traits.RedstoneChangedEventArgs
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.sounds.SoundEvents
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundSource
import net.minecraft.world.phys.AABB
import net.minecraft.core.{BlockPos, Direction}
import net.minecraft.world.phys.{BlockHitResult, Vec3}
import net.minecraftforge.api.distmarker.{Dist, OnlyIn}
import scala.jdk.CollectionConverters._

class Print(val canToggle: Option[() => Boolean], val scheduleUpdate: Option[Int => Unit], val onStateChange: Option[() => Unit]) extends traits.TileEntity with traits.RedstoneAware with traits.RotatableTile {
  def this() = this(None, None, None)
  def this(canToggle: () => Boolean, scheduleUpdate: Int => Unit, onStateChange: () => Unit) = this(Option(canToggle), Option(scheduleUpdate), Option(onStateChange))

  _isOutputEnabled = true

  val data = new PrintData()

  var boundsOff = ExtendedAABB.unitBounds
  var boundsOn = ExtendedAABB.unitBounds
  var state = false

  def bounds = if (state) boundsOn else boundsOff
  def noclip = if (state) data.noclipOn else data.noclipOff
  def shapes = if (state) data.stateOn else data.stateOff

  def isSideSolid(side: Direction): Boolean = {
    for (shape <- shapes if !Strings.isNullOrEmpty(shape.texture)) {
      val bounds = shape.bounds.rotateTowards(facing)
      val fullX = bounds.minX == 0 && bounds.maxX == 1
      val fullY = bounds.minY == 0 && bounds.maxY == 1
      val fullZ = bounds.minZ == 0 && bounds.maxZ == 1
      if (side match {
        case Direction.DOWN => bounds.minY == 0 && fullX && fullZ
        case Direction.UP => bounds.maxY == 1 && fullX && fullZ
        case Direction.NORTH => bounds.minZ == 0 && fullX && fullY
        case Direction.SOUTH => bounds.maxZ == 1 && fullX && fullY
        case Direction.WEST => bounds.minX == 0 && fullY && fullZ
        case Direction.EAST => bounds.maxX == 1 && fullY && fullZ
        case _ => false
      }) return true
    }
    false
  }

  def addCollisionBoxesToList(mask: AABB, list: util.List[AABB], pos: BlockPos = BlockPos.ZERO): Unit = {
    if (!noclip) {
      if (shapes.isEmpty) {
        val unitBounds = new AABB(0, 0, 0, 1, 1, 1).move(pos.getX, pos.getY, pos.getZ)
        if (mask == null || unitBounds.intersects(mask)) {
          list.add(unitBounds)
        }
      } else {
        for (shape <- shapes) {
          val bounds = shape.bounds.rotateTowards(facing).move(pos.getX, pos.getY, pos.getZ)
          if (mask == null || bounds.intersects(mask)) {
            list.add(bounds)
          }
        }
      }
    }
  }

  def rayTrace(start: Vec3, end: Vec3, pos: BlockPos = BlockPos.ZERO): BlockHitResult = {
    var closestDistance = Double.PositiveInfinity
    var closest: Option[Vec3] = None
    var closestDirection: Direction = Direction.UP
    if (shapes.isEmpty) {
      val bounds = new AABB(0, 0, 0, 1, 1, 1).move(pos.getX, pos.getY, pos.getZ)
      val hit = bounds.clip(start, end)
      if (hit.isPresent) {
        val hitVec = hit.get()
        val distance = hitVec.distanceTo(start)
        if (distance < closestDistance) {
          closestDistance = distance
          closest = Option(hitVec)
          closestDirection = Direction.getNearest(hitVec.x - pos.getX - 0.5, hitVec.y - pos.getY - 0.5, hitVec.z - pos.getZ - 0.5)
        }
      }
    } else {
      for (shape <- shapes) {
        val bounds = shape.bounds.rotateTowards(facing).move(pos.getX, pos.getY, pos.getZ)
        val hit = bounds.clip(start, end)
        if (hit.isPresent) {
          val hitVec = hit.get()
          val distance = hitVec.distanceTo(start)
          if (distance < closestDistance) {
            closestDistance = distance
            closest = Option(hitVec)
            closestDirection = Direction.getNearest(hitVec.x - pos.getX - 0.5, hitVec.y - pos.getY - 0.5, hitVec.z - pos.getZ - 0.5)
          }
        }
      }
    }
    closest.map(hitVec => new BlockHitResult(hitVec, closestDirection, pos, false)).orNull
  }

  def activate(): Boolean = {
    if (data.hasActiveState) {
      if (!state || !data.isButtonMode) {
        toggleState()
        return true
      }
    }
    false
  }

  private def buildValueSet(value: Int): util.Map[AnyRef, AnyRef] = {
    val map: util.Map[AnyRef, AnyRef] = new util.HashMap[AnyRef, AnyRef]()
    Direction.values().foreach {
      side => map.put(new java.lang.Integer(side.ordinal), new java.lang.Integer(value))
    }
    map
  }

  def toggleState(): Unit = {
    if (canToggle.fold(true)(_.apply())) {
      state = !state
      world.playSound(null, x + 0.5, y + 0.5, z + 0.5, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, if (state) 0.6F else 0.5F)
      world.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
      updateRedstone()
      if (state && data.isButtonMode) {
        val block = api.Items.get(Constants.BlockName.Print).block()
        val delay = block.tickRate(world)
        scheduleUpdate match {
          case Some(callback) => callback(delay)
          case _ => world.scheduleUpdate(getPos, block, delay)
        }
      }
      onStateChange.foreach(_.apply())
    }
  }

  def updateBounds(): Unit = {
    boundsOff = data.stateOff.drop(1).foldLeft(data.stateOff.headOption.fold(ExtendedAABB.unitBounds)(_.bounds))((a, b) => a.union(b.bounds))
    if (boundsOff.volume == 0) boundsOff = ExtendedAABB.unitBounds
    else boundsOff = boundsOff.rotateTowards(facing)
    boundsOn = data.stateOn.drop(1).foldLeft(data.stateOn.headOption.fold(ExtendedAABB.unitBounds)(_.bounds))((a, b) => a.union(b.bounds))
    if (boundsOn.volume == 0) boundsOn = ExtendedAABB.unitBounds
    else boundsOn = boundsOn.rotateTowards(facing)
  }

  def updateRedstone(): Unit = {
    if (data.emitRedstone) {
      setOutput(buildValueSet(if (data.emitRedstone(state)) data.redstoneLevel else 0))
    }
  }

  override protected def onRedstoneInputChanged(args: RedstoneChangedEventArgs): Unit = {
    val newState = args.newValue > 0
    if (!data.emitRedstone && data.hasActiveState && state != newState) {
      toggleState()
    }
  }

  override protected def onRotationChanged(): Unit = {
    super.onRotationChanged()
    updateBounds()
  }

  // ----------------------------------------------------------------------- //

  private final val DataTag = Settings.namespace + "data"
  private final val DataTagCompat = "data"
  private final val StateTag = Settings.namespace + "state"
  private final val StateTagCompat = "state"

  override def readFromNBTForServer(nbt: CompoundTag): Unit = {
    super.readFromNBTForServer(nbt)
    if (nbt.contains(DataTagCompat))
      data.load(nbt.getCompound(DataTagCompat))
    else
      data.load(nbt.getCompound(DataTag))
    if (nbt.contains(StateTagCompat))
      state = nbt.getBoolean(StateTagCompat)
    else
      state = nbt.getBoolean(StateTag)
    updateBounds()
  }

  override def writeToNBTForServer(nbt: CompoundTag): Unit = {
    super.writeToNBTForServer(nbt)
    nbt.setNewCompoundTag(DataTag, data.save)
    nbt.putBoolean(StateTag, state)
  }

  @OnlyIn(Dist.CLIENT)
  override def readFromNBTForClient(nbt: CompoundTag): Unit = {
    super.readFromNBTForClient(nbt)
    data.load(nbt.getCompound(DataTag))
    state = nbt.getBoolean(StateTag)
    updateBounds()
    if (world != null) {
      world.sendBlockUpdated(getPos, getLevel.getBlockState(getPos), getLevel.getBlockState(getPos), 3)
      if (data.emitLight) world.getChunkSource.getLightEngine.checkBlock(getPos)
    }
  }

  override def writeToNBTForClient(nbt: CompoundTag): Unit = {
    super.writeToNBTForClient(nbt)
    nbt.setNewCompoundTag(DataTag, data.save)
    nbt.putBoolean(StateTag, state)
  }
}

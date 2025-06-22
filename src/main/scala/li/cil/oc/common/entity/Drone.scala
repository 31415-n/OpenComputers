package li.cil.oc.common.entity

import java.lang
import java.lang.Iterable
import java.util.UUID

import li.cil.oc.Constants
import li.cil.oc.Localization
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Driver
import li.cil.oc.api.Machine
import li.cil.oc.api.driver.item
import li.cil.oc.api.internal
import li.cil.oc.api.internal.MultiTank
import li.cil.oc.api.machine
import li.cil.oc.api.machine.Context
import li.cil.oc.api.machine.MachineHost
import li.cil.oc.api.network._
import li.cil.oc.common.EventHandler
import li.cil.oc.common.GuiType
import li.cil.oc.common.inventory.ComponentInventory
import li.cil.oc.common.inventory.Inventory
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.integration.util.Wrench
import li.cil.oc.server.agent
import li.cil.oc.server.component
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ExtendedWorld._
import li.cil.oc.util.InventoryUtils
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import net.minecraftforge.fluids.IFluidTank
import net.minecraft.world.InteractionResult

import scala.jdk.CollectionConverters._

object Drone {
  // Entity type for 1.20.1 registration system
  lazy val ENTITY_TYPE = li.cil.oc.common.init.EntityTypes.DRONE
  
  val DataRunning: EntityDataAccessor[lang.Boolean] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.BOOLEAN)
  val DataTargetX: EntityDataAccessor[lang.Float] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.FLOAT)
  val DataTargetY: EntityDataAccessor[lang.Float] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.FLOAT)
  val DataTargetZ: EntityDataAccessor[lang.Float] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.FLOAT)
  val DataMaxAcceleration: EntityDataAccessor[lang.Float] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.FLOAT)
  val DataSelectedSlot: EntityDataAccessor[Integer] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.INT)
  val DataCurrentEnergy: EntityDataAccessor[Integer] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.INT)
  val DataMaxEnergy: EntityDataAccessor[Integer] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.INT)
  val DataStatusText: EntityDataAccessor[String] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.STRING)
  val DataInventorySize: EntityDataAccessor[Integer] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.INT)
  val DataLightColor: EntityDataAccessor[Integer] = SynchedEntityData.defineId(classOf[Drone], EntityDataSerializers.INT)
}

// internal.Rotatable is also in internal.Drone, but it wasn't since the start
// so this is to ensure it is implemented here, in the very unlikely case that
// someone decides to ship that specific version of the API.
class Drone(entityType: net.minecraft.world.entity.EntityType[_ <: Drone], level: Level) extends Entity(entityType, level) with MachineHost with internal.Drone with internal.Rotatable with Analyzable with Context {
  def this(level: Level) = this(li.cil.oc.common.init.EntityTypes.DRONE.get(), level)
  
  override def world: Level = level()

  // Some basic constants.
  val gravity = 0.05f
  // low for slow fall (float down)
  val drag = 0.8f
  val maxAcceleration = 0.1f
  val maxVelocity = 0.4f
  val maxInventorySize = 8
  // Set bounding box dimensions
  setBoundingBox(getBoundingBox().inflate(12 / 16f, 6 / 16f, 12 / 16f))
  // Fire immunity is set in EntityType definition for 1.20.1

  // Rendering stuff, purely eyecandy.
  val targetFlapAngles: Array[Array[Float]] = Array.fill(4, 2)(0f)
  val flapAngles: Array[Array[Float]] = Array.fill(4, 2)(0f)
  var nextFlapChange = 0
  var bodyAngle: Float = math.random.toFloat * 90
  var angularVelocity = 0f
  var nextAngularVelocityChange = 0
  var lastEnergyUpdate = 0

  // Logic stuff, components, machine and such.
  val info = new DroneData()
  val machine: api.machine.Machine = if (!level().isClientSide) {
    val m = Machine.create(this)
    m.node.asInstanceOf[Connector].setLocalBufferSize(0)
    m
  } else null
  val control: component.Drone = if (!level().isClientSide) new component.Drone(this) else null
  val components = new ComponentInventory {
    override def host: Drone = Drone.this

    override def items: Array[ItemStack] = info.components

    override def getSizeInventory: Int = info.components.length

    override def markDirty() {}

    override def isItemValidForSlot(slot: Int, stack: ItemStack) = true

    override def isUsableByPlayer(player: Player) = true

    override def node: Node = Option(machine).map(_.node).orNull

    override def onConnect(node: Node) {}

    override def onDisconnect(node: Node) {}

    override def onMessage(message: Message) {}
  }
  val equipmentInventory = new Inventory {
    val items = Array.empty[ItemStack]

    override def getSizeInventory = 0

    override def getInventoryStackLimit = 0

    override def markDirty(): Unit = {}

    override def isItemValidForSlot(slot: Int, stack: ItemStack) = false

    override def isUsableByPlayer(player: Player) = false
  }
  val mainInventory = new Inventory {
    val items: Array[ItemStack] = Array.fill[ItemStack](8)(ItemStack.EMPTY)

    override def getSizeInventory: Int = inventorySize

    override def getInventoryStackLimit = 64

    override def markDirty() {} // TODO update client GUI?

    override def isItemValidForSlot(slot: Int, stack: ItemStack): Boolean = slot >= 0 && slot < getSizeInventory

    override def isUsableByPlayer(player: Player): Boolean = player.distanceToSqr(Drone.this) < 64
  }
  val tank = new MultiTank {
    override def tankCount: Int = components.components.count {
      case Some(tank: IFluidTank) => true
      case _ => false
    }

    override def getFluidTank(index: Int): IFluidTank = components.components.collect {
      case Some(tank: IFluidTank) => tank
    }.apply(index)
  }
  var selectedTank = 0

  override def setSelectedTank(index: Int): Unit = selectedTank = index

  override def tier: Int = info.tier

  override def player(): Player = {
    agent.Player.updatePositionAndRotation(player_, facing, facing)
    agent.Player.setInventoryPlayerItems(player_)
    player_
  }

  override def name: String = info.name

  override def setName(name: String): Unit = info.name = name

  var ownerName: String = Settings.get.fakePlayerName

  var ownerUUID: UUID = Settings.get.fakePlayerProfile.getId

  private lazy val player_ = new agent.Player(this)

  // ----------------------------------------------------------------------- //
  // Forward context stuff to our machine. Interface needed for some components
  // to work correctly (such as the chunkloader upgrade).

  override def node: Node = machine.node

  override def canInteract(player: String): Boolean = machine.canInteract(player)

  override def isPaused: Boolean = machine.isPaused

  override def start(): Boolean = {
    if (level().isClientSide || machine.isRunning) {
      return false
    }
    preparePowerUp()
    machine.start()
  }

  override def pause(seconds: Double): Boolean = machine.pause(seconds)

  override def stop(): Boolean = machine.stop()

  override def consumeCallBudget(callCost: Double): Unit = machine.consumeCallBudget(callCost)

  override def signal(name: String, args: AnyRef*): Boolean = machine.signal(name, args: _*)

  // ----------------------------------------------------------------------- //

  override def getTarget = new Vec3(targetX.floatValue(), targetY.floatValue(), targetZ.floatValue())

  override def setTarget(value: Vec3): Unit = {
    targetX = value.x.toFloat
    targetY = value.y.toFloat
    targetZ = value.z.toFloat
  }

  override def getVelocity = new Vec3(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z)

  // ----------------------------------------------------------------------- //

  override def canBeCollidedWith = true

  override def canBePushed = true

  // ----------------------------------------------------------------------- //

  override def xPosition: Double = getX

  override def yPosition: Double = getY

  override def zPosition: Double = getZ

  override def markChanged() {}

  // ----------------------------------------------------------------------- //

  override def facing = Direction.SOUTH

  override def toLocal(value: Direction): Direction = value

  override def toGlobal(value: Direction): Direction = value

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: Player, side: Direction, hitX: Float, hitY: Float, hitZ: Float) = Array(machine.node)

  // ----------------------------------------------------------------------- //

  override def internalComponents(): Iterable[ItemStack] = asJavaIterable(info.components)

  override def componentSlot(address: String): Int = components.components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  override def onMachineConnect(node: Node) {}

  override def onMachineDisconnect(node: Node) {}

  def computeInventorySize(): Int = math.min(maxInventorySize, info.components.foldLeft(0)((acc, component) => acc + (Option(component) match {
    case Some(stack) => Option(Driver.driverFor(stack, getClass)) match {
      case Some(driver: item.Inventory) => math.max(1, driver.inventoryCapacity(stack) / 4)
      case _ => 0
    }
    case _ => 0
  })))

  // ----------------------------------------------------------------------- //

  override def defineSynchedData() {
    getEntityData.define(Drone.DataRunning, java.lang.Boolean.FALSE)
    getEntityData.define(Drone.DataTargetX, Float.box(0f))
    getEntityData.define(Drone.DataTargetY, Float.box(0f))
    getEntityData.define(Drone.DataTargetZ, Float.box(0f))
    getEntityData.define(Drone.DataMaxAcceleration, Float.box(0f))
    getEntityData.define(Drone.DataSelectedSlot, Int.box(0))
    getEntityData.define(Drone.DataCurrentEnergy, Int.box(0))
    getEntityData.define(Drone.DataMaxEnergy, Int.box(100))
    getEntityData.define(Drone.DataStatusText, "")
    getEntityData.define(Drone.DataInventorySize, Int.box(0))
    getEntityData.define(Drone.DataLightColor, Int.box(0x66DD55))
  }

  def initializeAfterPlacement(stack: ItemStack, player: Player, position: Vec3) {
    info.load(stack)
    control.node.changeBuffer(info.storedEnergy - control.node.localBuffer)
    wireThingsTogether()
    inventorySize = computeInventorySize()
    setPos(position.x, position.y, position.z)
  }

  def preparePowerUp() {
    targetX = math.floor(getX).toFloat + 0.5f
    targetY = math.round(getY).toFloat + 0.5f
    targetZ = math.floor(getZ).toFloat + 0.5f
    targetAcceleration = maxAcceleration

    wireThingsTogether()
  }

  private def wireThingsTogether(): Unit = {
    api.Network.joinNewNetwork(machine.node)
    machine.node.connect(control.node)
    machine.setCostPerTick(Settings.get.droneCost)
    components.connectComponents()
  }

  def isRunning: Boolean = getEntityData.get(Drone.DataRunning)

  def targetX: lang.Float = getEntityData.get(Drone.DataTargetX)

  def targetY: lang.Float = getEntityData.get(Drone.DataTargetY)

  def targetZ: lang.Float = getEntityData.get(Drone.DataTargetZ)

  def targetAcceleration: lang.Float = getEntityData.get(Drone.DataMaxAcceleration)

  def selectedSlot: Int = getEntityData.get(Drone.DataSelectedSlot) & 0xFF

  def globalBuffer: Integer = getEntityData.get(Drone.DataCurrentEnergy)

  def globalBufferSize: Integer = getEntityData.get(Drone.DataMaxEnergy)

  def statusText: String = getEntityData.get(Drone.DataStatusText)

  def inventorySize: Int = getEntityData.get(Drone.DataInventorySize) & 0xFF

  def lightColor: Integer = getEntityData.get(Drone.DataLightColor)

  def setRunning(value: Boolean): Unit = getEntityData.set(Drone.DataRunning, Boolean.box(value))

  // Round target values to low accuracy to avoid floating point errors accumulating.
  def targetX_=(value: Float): Unit = getEntityData.set(Drone.DataTargetX, Float.box(math.round(value * 4) / 4f))

  def targetY_=(value: Float): Unit = getEntityData.set(Drone.DataTargetY, Float.box(math.round(value * 4) / 4f))

  def targetZ_=(value: Float): Unit = getEntityData.set(Drone.DataTargetZ, Float.box(math.round(value * 4) / 4f))

  def targetAcceleration_=(value: Float): Unit = getEntityData.set(Drone.DataMaxAcceleration, Float.box(math.max(0, math.min(maxAcceleration, value))))

  def setSelectedSlot(value: Int): Unit = getEntityData.set(Drone.DataSelectedSlot, Int.box(value.toByte))

  def globalBuffer_=(value: Int): Unit = getEntityData.set(Drone.DataCurrentEnergy, Int.box(value))

  def globalBufferSize_=(value: Int): Unit = getEntityData.set(Drone.DataMaxEnergy, Int.box(value))

  def statusText_=(value: String): Unit = getEntityData.set(Drone.DataStatusText, Option(value).fold("")(_.lines.map(_.take(10)).take(2).mkString("\n")))

  def inventorySize_=(value: Int): Unit = getEntityData.set(Drone.DataInventorySize, Int.box(value.toByte))

  def lightColor_=(value: Int): Unit = getEntityData.set(Drone.DataLightColor, Int.box(value))

  override def setPositionAndRotationDirect(x: Double, y: Double, z: Double, yaw: Float, pitch: Float, posRotationIncrements: Int, teleport: Boolean): Unit = {
    // Only set exact position if we're too far away from the server's
    // position, otherwise keep interpolating. This removes jitter and
    // is good enough for drones.
    if (!isRunning || distanceToSqr(x, y, z) > 1) {
      super.setPositionAndRotation(x, y, z, yaw, pitch)
    }
    else {
      targetX = x.toFloat
      targetY = y.toFloat
      targetZ = z.toFloat
    }
  }

  override def tick() {
    super.tick()

    if (!level().isClientSide) {
      if (isInWater() || isInLava()) {
        // We're not water-proof!
        machine.stop()
      }
      machine.update()
      components.updateComponents()
      setRunning(machine.isRunning)

      val buffer = math.round(machine.node.asInstanceOf[Connector].globalBuffer).toInt
      if (math.abs(lastEnergyUpdate - buffer) > 1 || level().getGameTime % 200 == 0) {
        lastEnergyUpdate = buffer
        globalBuffer = buffer
        globalBufferSize = machine.node.asInstanceOf[Connector].globalBufferSize.toInt
      }
    }
    else {
      if (isRunning) {
        // Client side update; occasionally update wing pitch and rotation to
        // make the drones look a bit more dynamic.
        val rng = level().random
        nextFlapChange -= 1
        nextAngularVelocityChange -= 1

        if (nextFlapChange < 0) {
          nextFlapChange = 5 + rng.nextInt(10)
          for (i <- 0 until 2) {
            val flap = rng.nextInt(targetFlapAngles.length)
            targetFlapAngles(flap)(0) = math.toRadians(rng.nextFloat() * 4 - 2).toFloat
            targetFlapAngles(flap)(1) = math.toRadians(rng.nextFloat() * 4 - 2).toFloat
          }
        }

        if (nextAngularVelocityChange < 0) {
          if (angularVelocity != 0) {
            angularVelocity = 0
            nextAngularVelocityChange = 20
          }
          else {
            angularVelocity = if (rng.nextBoolean()) 0.1f else -0.1f
            nextAngularVelocityChange = 100
          }
        }

        // Interpolate wing rotations.
        (flapAngles, targetFlapAngles).zipped.foreach((f, t) => {
          f(0) = f(0) * 0.7f + t(0) * 0.3f
          f(1) = f(1) * 0.7f + t(1) * 0.3f
        })

        // Update body rotation.
        bodyAngle += angularVelocity
      }
    }

    xOld = getX
    yOld = getY
    zOld = getZ
    noPhysics = !level().noCollision(this, getBoundingBox())

    if (isRunning) {
      val toTarget = new Vec3(targetX - getX, targetY - getY, targetZ - getZ)
      val distance = toTarget.length()
      val velocity = getDeltaMovement()
      if (distance > 0 && (distance > 0.005f || velocity.dotProduct(velocity) > 0.005f)) {
        val acceleration = math.min(targetAcceleration.floatValue(), distance) / distance
        val velocityX = velocity.x + toTarget.x * acceleration
        val velocityY = velocity.y + toTarget.y * acceleration
        val velocityZ = velocity.z + toTarget.z * acceleration
        setDeltaMovement(math.max(-maxVelocity, math.min(maxVelocity, velocityX)), math.max(-maxVelocity, math.min(maxVelocity, velocityY)), math.max(-maxVelocity, math.min(maxVelocity, velocityZ)))
      }
      else {
        setDeltaMovement(0, 0, 0)
        setPos(targetX.floatValue(), targetY.floatValue(), targetZ.floatValue())
      }
    }
    else {
      // No power, free fall: engage!
      setDeltaMovement(getDeltaMovement().add(0, -gravity, 0))
    }

    move(MoverType.SELF, getDeltaMovement())

    // Make sure we don't get infinitely faster.
    if (isRunning) {
      setDeltaMovement(getDeltaMovement().scale(drag))
    }
    else {
      val groundDrag = level().getBlockState(BlockPosition(this: Entity).offset(Direction.DOWN)).getBlock.getFriction() * drag
      val currentMovement = getDeltaMovement()
      setDeltaMovement(currentMovement.x * groundDrag, currentMovement.y * drag, currentMovement.z * groundDrag)
      if (onGround()) {
        setDeltaMovement(getDeltaMovement().multiply(1, -0.5, 1))
      }
    }
  }

  override def hitByEntity(entity: Entity): Boolean = {
    if (isRunning) {
      val direction = new Vec3(entity.getX - getX, entity.getY + entity.getEyeHeight() - getY, entity.getZ - getZ).normalize()
      if (!level().isClientSide) {
        if (Settings.get.inputUsername)
          machine.signal("hit", Double.box(direction.x), Double.box(direction.z), Double.box(direction.y), entity.getName)
        else
          machine.signal("hit", Double.box(direction.x), Double.box(direction.z), Double.box(direction.y))
      }
      val currentMovement = getDeltaMovement()
      setDeltaMovement((currentMovement.x - direction.x) * 0.5f, (currentMovement.y - direction.y) * 0.5f, (currentMovement.z - direction.z) * 0.5f)
    }
    super.hitByEntity(entity)
  }

  override def interact(player: Player, hand: InteractionHand): net.minecraft.world.InteractionResult = {
    if (isRemoved()) return net.minecraft.world.InteractionResult.FAIL
    if (player.isSneaking) {
      if (Wrench.isWrench(player.getHeldItemMainhand)) {
        if(!level().isClientSide) {
          outOfWorld()
        }
      }
      else if (!level().isClientSide && !machine.isRunning) {
        start()
      }
    }
    else if (!level().isClientSide) {
      // GUI opening needs to be handled differently in 1.20.1
      // player.openGui(OpenComputers, GuiType.Drone.id, level(), getId, 0, 0)
    }
    net.minecraft.world.InteractionResult.SUCCESS
  }

  // No step sounds. Except on that one day.
  override def playStepSound(pos: BlockPos, block: Block): Unit = {
    if (EventHandler.isItTime) super.playStepSound(pos, block)
  }

  // ----------------------------------------------------------------------- //

  private var isChangingDimension = false

  override def changeDimension(dimension: Int): Entity = {
    // Store relative target as target, to allow adding that in our "new self"
    // (entities get re-created after changing dimension).
    targetX = (targetX - getX).toFloat
    targetY = (targetY - getY).toFloat
    targetZ = (targetZ - getZ).toFloat
    try {
      isChangingDimension = true
      super.changeDimension(dimension)
    }
    finally {
      isChangingDimension = false
      remove(Entity.RemovalReason.DISCARDED) // Again, to actually close old machine state after copying it.
    }
  }

  override def copyDataFromOld(entity: Entity): Unit = {
    super.copyDataFromOld(entity)
    // Compute relative target based on old position and update, because our
    // frame of reference most certainly changed (i.e. we'll spawn at different
    // coordinates than the ones we started traveling from, e.g. when porting
    // to the nether it'll be oldpos / 8).
    entity match {
      case drone: Drone =>
        targetX = (getX + drone.targetX).toFloat
        targetY = (getY + drone.targetY).toFloat
        targetZ = (getZ + drone.targetZ).toFloat
      case _ =>
        targetX = getX.toFloat
        targetY = getY.toFloat
        targetZ = getZ.toFloat
    }
  }

  override def remove(reason: Entity.RemovalReason) {
    super.remove(reason)
    if (!level().isClientSide && !isChangingDimension) {
      machine.stop()
      machine.node.remove()
      components.disconnectComponents()
      components.saveComponents()
    }
  }

  override def outOfWorld(): Unit = {
    if (isRemoved()) return
    super.remove(Entity.RemovalReason.DISCARDED)
    if (!world.isRemote) {
      val stack = api.Items.get(Constants.ItemName.Drone).createItemStack(1)
      info.storedEnergy = control.node.localBuffer.toInt
      info.save(stack)
      val entity = new net.minecraft.world.entity.item.ItemEntity(level(), getX, getY, getZ, stack)
      entity.setPickupDelay(15)
      level().addFreshEntity(entity)
      InventoryUtils.dropAllSlots(BlockPosition(this: Entity), mainInventory)
    }
  }

  override def getName: String = Localization.localizeImmediately("entity.oc.Drone.name")

  override def updateFluidHeightAndDoFluidPushing(): Boolean = {
    val result = super.updateFluidHeightAndDoFluidPushing()
    result
  }

  override def readAdditionalSaveData(nbt: CompoundTag) {
    info.load(nbt.getCompoundTag("info"))
    inventorySize = computeInventorySize()
    if (!level().isClientSide) {
      machine.load(nbt.getCompoundTag("machine"))
      control.load(nbt.getCompoundTag("control"))
      components.load(nbt.getCompoundTag("components"))
      mainInventory.load(nbt.getCompoundTag("inventory"))

      wireThingsTogether()
    }
    targetX = nbt.getFloat("targetX")
    targetY = nbt.getFloat("targetY")
    targetZ = nbt.getFloat("targetZ")
    targetAcceleration = nbt.getFloat("targetAcceleration")
    setSelectedSlot(nbt.getByte("selectedSlot") & 0xFF)
    setSelectedTank(nbt.getByte("selectedTank") & 0xFF)
    statusText = nbt.getString("statusText")
    lightColor = nbt.getInteger("lightColor")
    if (nbt.hasKey("owner")) {
      ownerName = nbt.getString("owner")
    }
    if (nbt.hasKey("ownerUuid")) {
      ownerUUID = UUID.fromString(nbt.getString("ownerUuid"))
    }
  }

  override def addAdditionalSaveData(nbt: CompoundTag) {
    if (level().isClientSide) return
    components.saveComponents()
    info.storedEnergy = globalBuffer.toInt
    nbt.setNewCompoundTag("info", info.save)
    if (!level().isClientSide) {
      nbt.setNewCompoundTag("machine", machine.save)
      nbt.setNewCompoundTag("control", control.save)
      nbt.setNewCompoundTag("components", components.save)
      nbt.setNewCompoundTag("inventory", mainInventory.save)
    }
    nbt.setFloat("targetX", targetX)
    nbt.setFloat("targetY", targetY)
    nbt.setFloat("targetZ", targetZ)
    nbt.setFloat("targetAcceleration", targetAcceleration)
    nbt.setByte("selectedSlot", selectedSlot.toByte)
    nbt.setByte("selectedTank", selectedTank.toByte)
    nbt.setString("statusText", statusText)
    nbt.setInteger("lightColor", lightColor)
    nbt.setString("owner", ownerName)
    nbt.setString("ownerUuid", ownerUUID.toString)
  }
}

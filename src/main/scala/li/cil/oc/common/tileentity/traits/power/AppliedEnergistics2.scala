package li.cil.oc.common.tileentity.traits.power
import java.util

// AppliedEnergistics2 integration temporarily disabled for 1.20.1 port
import li.cil.oc.Settings
import li.cil.oc.common.EventHandler
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.Power
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraftforge.fml.common._

import scala.collection.JavaConversions

trait AppliedEnergistics2 extends Common {
  private def useAppliedEnergistics2Power() = isServer && Mods.AppliedEnergistics2.isModAvailable

  // 'Manual' lazy val, because lazy vals mess up the class loader, leading to class not found exceptions.
  private var node: Option[AnyRef] = None
  private var gridNodeStateUpdateRequested: Boolean = false

  def requestGridNodeStateUpdate(): Unit = {
    if (!gridNodeStateUpdateRequested) {
      EventHandler.scheduleAE2Add(this)
      gridNodeStateUpdateRequested = true
    }
  }

  def updateGridNodeState(): Unit = {
    if (!this.isInvalid) {
      val gridNode = getGridNode(AEPartLocation.INTERNAL)
      if (gridNode != null) {
        gridNode.updateState()
        gridNodeStateUpdateRequested = false
      }
    }
  }

  override def updateEntity() {
    super.updateEntity()
    if (useAppliedEnergistics2Power() && getLevel.getGameTime % Settings.get.tickFrequency == 0) {
      updateEnergy()
    }
  }

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  private def updateEnergy() {
    tryAllSides((demand, _) => {
      val grid = getGridNode(AEPartLocation.INTERNAL).getGrid
      if (grid != null) {
        val cache = grid.getCache(classOf[IEnergyGrid]).asInstanceOf[IEnergyGrid]
        if (cache != null) {
          cache.extractAEPower(demand, Actionable.MODULATE, PowerMultiplier.CONFIG)
        }
        else 0.0
      }
      else 0.0
    }, Power.fromAE, Power.toAE)
  }

  override def validate() {
    super.validate()
    if (useAppliedEnergistics2Power()) requestGridNodeStateUpdate()
  }

  override def invalidate() {
    super.invalidate()
    if (useAppliedEnergistics2Power()) securityBreak()
  }

  override def onChunkUnload() {
    super.onChunkUnload()
    if (useAppliedEnergistics2Power()) securityBreak()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBTForServer(nbt: CompoundTag) {
    super.readFromNBTForServer(nbt)
    if (useAppliedEnergistics2Power()) loadNode(nbt)
  }

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  private def loadNode(nbt: CompoundTag): Unit = {
    getGridNode(AEPartLocation.INTERNAL).loadFromNBT(Settings.namespace + "ae2power", nbt)
  }

  override def setLevel(levelIn: Level): Unit = {
    if (getLevel == levelIn)
      return
    super.setLevel(levelIn)
    if (levelIn != null && isServer && useAppliedEnergistics2Power) {
      requestGridNodeStateUpdate()
    }
  }

  override def writeToNBTForServer(nbt: CompoundTag) {
    super.writeToNBTForServer(nbt)
    if (useAppliedEnergistics2Power()) saveNode(nbt)
  }

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  private def saveNode(nbt: CompoundTag): Unit = {
    getGridNode(AEPartLocation.INTERNAL).saveToNBT(Settings.namespace + "ae2power", nbt)
  }

  // ----------------------------------------------------------------------- //

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def getGridNode(side: AEPartLocation): IGridNode = node match {
    case Some(gridNode: IGridNode) => gridNode
    case _ if isServer =>
      val gridNode = AEApi.instance.grid.createGridNode(new AppliedEnergistics2GridBlock(this))
      node = Option(gridNode)
      gridNode
    case _ => null
  }

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def getCableConnectionType(side: AEPartLocation): AECableType = AECableType.SMART

  @Optional.Method(modid = Mods.IDs.AppliedEnergistics2)
  def securityBreak() {
    getGridNode(AEPartLocation.INTERNAL).destroy()
  }
}

// AppliedEnergistics2GridBlock temporarily disabled
class AppliedEnergistics2GridBlock(val tileEntity: AppliedEnergistics2) {
  override def getIdlePowerUsage: Double = 0.0

  override def getFlags: util.EnumSet[GridFlags] = util.EnumSet.noneOf(classOf[GridFlags])

  def isWorldAccessible: Boolean = true

  override def getLocation: DimensionalCoord = new DimensionalCoord(tileEntity)

  override def getGridColor: AEColor = AEColor.TRANSPARENT

  override def onGridNotification(p1: GridNotification): Unit = {}

  override def setNetworkStatus(p1: IGrid, p2: Int): Unit = {}

  override def getConnectableSides: util.EnumSet[Direction] = {
    val connectableSides = JavaConversions.asJavaCollection(Direction.values.filter(tileEntity.canConnectPower))
    if (connectableSides.isEmpty) {
      val s = util.EnumSet.copyOf(JavaConversions.asJavaCollection(Direction.values))
      s.clear()
      s
    }
    else
      util.EnumSet.copyOf(connectableSides)
  }

  override def getMachine: IGridHost = tileEntity.asInstanceOf[IGridHost]

  override def gridChanged(): Unit = {}

  override def getMachineRepresentation: ItemStack = ItemStack.EMPTY
}

package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import li.cil.oc.integration.util.BundledRedstone
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.integration.Mods
// ProjectRed integration temporarily disabled for 1.20.1 port
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntArrayTag
import net.minecraft.core.Direction
import net.minecraft.nbt.Tag
import net.minecraftforge.fml.common.Optional
import java.util

import li.cil.oc.integration.charset.{CapabilitiesCharset, ModCharset}
import net.minecraftforge.common.capabilities.Capability

@Optional.Interface(iface = "mrtjp.projectred.api.IBundledTile", modid = Mods.IDs.ProjectRedCore)
trait BundledRedstoneAware extends RedstoneAware {

  protected[tileentity] val _bundledInput: Array[Array[Int]] = Array.fill(6)(Array.fill(16)(-1))

  protected[tileentity] val _rednetInput: Array[Array[Int]] = Array.fill(6)(Array.fill(16)(-1))

  protected[tileentity] val _bundledOutput: Array[Array[Int]] = Array.fill(6)(Array.fill(16)(0))

  // ----------------------------------------------------------------------- //

  override def setOutputEnabled(value: Boolean): RedstoneAware = {
    if (value != _isOutputEnabled) {
      if (!value) {
        for (i <- _bundledOutput.indices) {
          for (j <- _bundledOutput(i).indices) {
            _bundledOutput(i)(j) = 0
          }
        }
      }
    }
    super.setOutputEnabled(value)
  }

  def getBundledInput: Array[Array[Int]] = {
    (0 until 6).map(side => (0 until 16).map(color => _bundledInput(side)(color) max _rednetInput(side)(color) max 0).toArray).toArray
  }

  private def checkSide(side: Direction): Int = {
    val index = side.ordinal
    if (index >= 6) throw new IndexOutOfBoundsException(s"Bad side $side")
    index
  }

  private def checkColor(color: Int): Int = {
    if (color < 0 || color >= 16) throw new IndexOutOfBoundsException(s"Bad color $color")
    color
  }

  def getBundledInput(side: Direction): Array[Int] = {
    val sideIndex = checkSide(side)
    val bundled = _bundledInput(sideIndex)
    val rednet = _rednetInput(sideIndex)
    (bundled, rednet).zipped.map((a, b) => a max b max 0)
  }

  def getBundledInput(side: Direction, color: Int): Int = {
    val sideIndex = checkSide(side)
    val colorIndex = checkColor(color)
    val bundled = _bundledInput(sideIndex)(colorIndex)
    val rednet = _rednetInput(sideIndex)(colorIndex)
    bundled max rednet max 0
  }

  def setBundledInput(side: Direction, color: Int, newValue: Int): Unit = {
    updateInput(_bundledInput, side, color, newValue)
  }

  def setBundledInput(side: Direction, newBundledInput: Array[Int]): Unit = {
    for (color <- 0 until 16) {
      val value = if (newBundledInput == null || color >= newBundledInput.length) 0 else newBundledInput(color)
      setBundledInput(side, color, value)
    }
  }

  def setRednetInput(side: Direction, color: Int, value: Int): Unit = updateInput(_rednetInput, side, color, value)

  def updateInput(inputs: Array[Array[Int]], side: Direction, color: Int, newValue: Int): Unit = {
    val sideIndex = checkSide(side)
    val colorIndex = checkColor(color)
    val oldValue = inputs(sideIndex)(colorIndex)
    if (oldValue != newValue) {
      inputs(sideIndex)(colorIndex) = newValue
      if (oldValue != -1) {
        onRedstoneInputChanged(RedstoneChangedEventArgs(side, oldValue, newValue, colorIndex))
      }
    }
  }

  def getBundledOutput: Array[Array[Int]] = _bundledInput

  def getBundledOutput(side: Direction): Array[Int] = _bundledOutput(checkSide(toLocal(side)))

  def getBundledOutput(side: Direction, color: Int): Int = getBundledOutput(side)(checkColor(color))

  def setBundledOutput(side: Direction, color: Int, value: Int): Boolean = if (value != getBundledOutput(side, color)) {
    _bundledOutput(checkSide(toLocal(side)))(checkColor(color)) = value
    onRedstoneOutputChanged(side)
    true
  } else false

  def setBundledOutput(side: Direction, values: util.Map[_, _]): Boolean = {
    val sideIndex = toLocal(side).ordinal
    var changed: Boolean = false
    (0 until 16).foreach(color => {
      // due to a bug in our jnlua layer, I cannot loop the map
      valueToInt(getObjectFuzzy(values, color)) match {
        case Some(newValue: Int) =>
          if (newValue != getBundledOutput(side, color)) {
            _bundledOutput(sideIndex)(color) = newValue
            changed = true
          }
        case _ =>
      }
    })
    if (changed) {
      onRedstoneOutputChanged(side)
    }
    changed
  }

  def setBundledOutput(values: util.Map[_, _]): Boolean = {
    var changed: Boolean = false
    Direction.values.foreach(side => {
      val sideIndex = toLocal(side).ordinal
      // due to a bug in our jnlua layer, I cannot loop the map
      getObjectFuzzy(values, sideIndex) match {
        case Some(child: util.Map[_, _]) if setBundledOutput(side, child) => changed = true
        case _ =>
      }
    })
    changed
  }

  // ----------------------------------------------------------------------- //

  override def updateRedstoneInput(side: Direction) {
    super.updateRedstoneInput(side)
    setBundledInput(side, BundledRedstone.computeBundledInput(position, side))
  }

  // ----------------------------------------------------------------------- //

  private final val BundledInputTag = Settings.namespace + "rs.bundledInput"
  private final val BundledOutputTag = Settings.namespace + "rs.bundledOutput"
  private final val RednetInputTag = Settings.namespace + "rs.rednetInput"

  override def readFromNBTForServer(nbt: CompoundTag) {
    super.readFromNBTForServer(nbt)

    nbt.getTagList(BundledInputTag, Tag.TAG_INT_ARRAY).toArray[IntArrayTag].
      map(_.getAsIntArray).zipWithIndex.foreach {
      case (input, index) if index < _bundledInput.length =>
        val safeLength = input.length min _bundledInput(index).length
        input.copyToArray(_bundledInput(index), 0, safeLength)
      case _ =>
    }
    nbt.getTagList(BundledOutputTag, Tag.TAG_INT_ARRAY).toArray[IntArrayTag].
      map(_.getAsIntArray).zipWithIndex.foreach {
      case (input, index) if index < _bundledOutput.length =>
        val safeLength = input.length min _bundledOutput(index).length
        input.copyToArray(_bundledOutput(index), 0, safeLength)
      case _ =>
    }

    nbt.getTagList(RednetInputTag, Tag.TAG_INT_ARRAY).toArray[IntArrayTag].
      map(_.getAsIntArray).zipWithIndex.foreach {
      case (input, index) if index < _rednetInput.length =>
        val safeLength = input.length min _rednetInput(index).length
        input.copyToArray(_rednetInput(index), 0, safeLength)
      case _ =>
    }
  }

  override def writeToNBTForServer(nbt: CompoundTag) {
    super.writeToNBTForServer(nbt)

    nbt.setNewTagList(BundledInputTag, _bundledInput.view)
    nbt.setNewTagList(BundledOutputTag, _bundledOutput.view)

    nbt.setNewTagList(RednetInputTag, _rednetInput.view)
  }

  override def hasCapability(capability: Capability[_], side: Direction): Boolean = {
    if (capability == CapabilitiesCharset.BUNDLED_EMITTER || capability == CapabilitiesCharset.BUNDLED_RECEIVER) {
      true
    } else {
      super.hasCapability(capability, side)
    }
  }

  override def getCapability[T](capability: Capability[T], side: Direction): T = {
    if (capability == CapabilitiesCharset.BUNDLED_EMITTER || capability == CapabilitiesCharset.BUNDLED_RECEIVER) {
      new ModCharset.BundledRedstoneView(getBundledOutput(side), () => setBundledInput(side, BundledRedstone.computeBundledInput(position, side))).asInstanceOf[T]
    } else {
      super.getCapability(capability, side)
    }
  }

  @Optional.Method(modid = Mods.IDs.ProjectRedCore)
  override def canConnectBundled(side: Int): Boolean = isOutputEnabled

  @Optional.Method(modid = Mods.IDs.ProjectRedCore)
  override def getBundledSignal(side: Int): Array[Byte] = getBundledOutput(Direction.from3DDataValue(side)).map(value => math.min(math.max(value, 0), 255).toByte)
}

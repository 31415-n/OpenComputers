package li.cil.oc.common.tileentity.traits

import li.cil.oc.Settings
import net.minecraft.nbt.CompoundTag
import net.minecraft.core.Direction
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn

/**
  * @author Vexatos
  */
trait OpenSides extends TileEntity {
  protected def SideCount = Direction.values().length

  protected def defaultState: Boolean = false

  var openSides = Array.fill(SideCount)(defaultState)

  def compressSides = (Direction.values(), openSides).zipped.foldLeft(0)((acc, entry) => acc | (if (entry._2) 1 << entry._1.ordinal() else 0)).toByte

  def uncompressSides(byte: Byte) = Direction.values().map(d => ((1 << d.ordinal()) & byte) != 0)

  def isSideOpen(side: Direction) = side != null && openSides(side.ordinal())

  def setSideOpen(side: Direction, value: Boolean): Unit = if (side != null && openSides(side.ordinal()) != value) {
    openSides(side.ordinal()) = value
  }

  override def readFromNBTForServer(nbt: CompoundTag) {
    super.readFromNBTForServer(nbt)
    if (nbt.contains(Settings.namespace + "openSides"))
      openSides = uncompressSides(nbt.getByte(Settings.namespace + "openSides"))
  }

  override def writeToNBTForServer(nbt: CompoundTag) {
    super.writeToNBTForServer(nbt)
    nbt.putByte(Settings.namespace + "openSides", compressSides)
  }

  @OnlyIn(Dist.CLIENT)
  override def readFromNBTForClient(nbt: CompoundTag) {
    super.readFromNBTForClient(nbt)
    openSides = uncompressSides(nbt.getByte(Settings.namespace + "openSides"))
  }

  override def writeToNBTForClient(nbt: CompoundTag) {
    super.writeToNBTForClient(nbt)
    nbt.putByte(Settings.namespace + "openSides", compressSides)
  }
}

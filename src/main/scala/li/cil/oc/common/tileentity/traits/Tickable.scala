package li.cil.oc.common.tileentity.traits

import net.minecraft.world.level.block.entity.BlockEntityTicker

trait Tickable extends TileEntity {
  def tick(): Unit = updateEntity()
}

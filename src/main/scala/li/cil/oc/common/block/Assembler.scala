package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

class Assembler extends SimpleBlock with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  override def propagatesSkylightDown(state: BlockState, world: BlockGetter, pos: BlockPos): Boolean = true

  override def useShapeForLightOcclusion(state: BlockState): Boolean = true

  override def isFaceSturdy(state: BlockState, world: BlockGetter, pos: BlockPos, side: Direction): Boolean = 
    side == Direction.DOWN || side == Direction.UP

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.assemblerRate

  override def guiType = GuiType.Assembler

  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = new tileentity.Assembler(pos, state)
}

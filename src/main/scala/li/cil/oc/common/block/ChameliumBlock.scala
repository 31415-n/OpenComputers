package li.cil.oc.common.block

import net.minecraft.world.level.material.Material
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.item.DyeColor

object ChameliumBlock {
  final val Color = EnumProperty.create("color", classOf[DyeColor])
}

class ChameliumBlock extends SimpleBlock(Material.STONE) {
  registerDefaultState(stateDefinition.any().setValue(ChameliumBlock.Color, DyeColor.BLACK))

  // Block state methods are handled differently in 1.20.1
  // Meta-based methods are no longer used

  override def createBlockStateDefinition(builder: StateDefinition.Builder[net.minecraft.world.level.block.Block, BlockState]): Unit = {
    builder.add(ChameliumBlock.Color)
  }

  override def hasBlockEntity(state: BlockState): Boolean = false
}

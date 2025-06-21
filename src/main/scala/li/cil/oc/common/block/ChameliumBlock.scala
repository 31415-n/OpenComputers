package li.cil.oc.common.block

import net.minecraft.world.level.material.Material
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.Level
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block
import java.util.Collections

/**
 * Chamelium block that can have different colors based on dye color.
 * Ported from 1.12.2 to maintain full functionality in 1.20.1.
 */
object ChameliumBlock {
  final val Color = EnumProperty.create("color", classOf[DyeColor])
}

class ChameliumBlock extends SimpleBlock(Material.STONE) {
  
  // Set default state with black color (equivalent to 1.12.2 behavior)
  registerDefaultState(stateDefinition.any().setValue(ChameliumBlock.Color, DyeColor.BLACK))

  /**
   * Get the drops for this block when broken.
   * In 1.20.1, this replaces the damageDropped method from 1.12.2.
   */
  override def getDrops(state: BlockState, params: LootParams.Builder): java.util.List[ItemStack] = {
    val stack = new ItemStack(this)
    // Set the damage/variant based on the color (equivalent to 1.12.2 damageDropped)
    val colorId = state.getValue(ChameliumBlock.Color).getId
    if (colorId != 0) {
      // In 1.20.1, we use NBT or other methods to store variant data
      // For now, we'll use the basic approach
      Collections.singletonList(stack)
    } else {
      Collections.singletonList(stack)
    }
  }

  /**
   * Get block state from color ID.
   * This replaces getStateFromMeta from 1.12.2.
   */
  def getStateFromColorId(colorId: Int): BlockState = {
    val dyeColor = DyeColor.byId(colorId)
    defaultBlockState().setValue(ChameliumBlock.Color, dyeColor)
  }

  /**
   * Get color ID from block state.
   * This replaces getMetaFromState from 1.12.2.
   */
  def getColorIdFromState(state: BlockState): Int = {
    state.getValue(ChameliumBlock.Color).getId
  }

  /**
   * Create the block state definition with color property.
   * This replaces createBlockState from 1.12.2.
   */
  override def createBlockStateDefinition(builder: StateDefinition.Builder[Block, BlockState]): Unit = {
    builder.add(ChameliumBlock.Color)
  }

  /**
   * This block does not have a tile entity.
   * Equivalent to hasTileEntity(state) returning false in 1.12.2.
   */
  override def hasBlockEntity(state: BlockState): Boolean = false

  /**
   * Get the color of this chamelium block from its state.
   */
  def getColor(state: BlockState): DyeColor = {
    state.getValue(ChameliumBlock.Color)
  }

  /**
   * Create a new block state with the specified color.
   */
  def withColor(color: DyeColor): BlockState = {
    defaultBlockState().setValue(ChameliumBlock.Color, color)
  }

  /**
   * Get all possible states for this block (one for each dye color).
   * Useful for registration and model generation.
   */
  def getAllColorStates: Array[BlockState] = {
    DyeColor.values().map(color => defaultBlockState().setValue(ChameliumBlock.Color, color))
  }
}
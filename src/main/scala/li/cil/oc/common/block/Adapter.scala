package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.Wrench
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.BlockHitResult

class Adapter extends SimpleBlock with traits.GUI {
  override def guiType = GuiType.Adapter

  override def newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity = new tileentity.Adapter(pos, state)

  // ----------------------------------------------------------------------- //

  override def neighborChanged(state: BlockState, world: Level, pos: BlockPos, block: Block, fromPos: BlockPos, isMoving: Boolean): Unit =
    world.getBlockEntity(pos) match {
      case adapter: tileentity.Adapter => adapter.neighborChanged()
      case _ => // Ignore.
    }

  override def onNeighborChange(state: BlockState, world: Level, pos: BlockPos, neighbor: BlockPos): Unit =
    world.getBlockEntity(pos) match {
      case adapter: tileentity.Adapter =>
        val side =
          if (neighbor == pos.below()) Direction.DOWN
          else if (neighbor == pos.above()) Direction.UP
          else if (neighbor == pos.north()) Direction.NORTH
          else if (neighbor == pos.south()) Direction.SOUTH
          else if (neighbor == pos.west()) Direction.WEST
          else if (neighbor == pos.east()) Direction.EAST
          else throw new IllegalArgumentException("not a neighbor")
        adapter.neighborChanged(side)
      case _ => // Ignore.
    }

  override def use(state: BlockState, world: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult = {
    val heldItem = player.getItemInHand(hand)
    val side = hit.getDirection
    val hitX = hit.getLocation.x.toFloat
    val hitY = hit.getLocation.y.toFloat
    val hitZ = hit.getLocation.z.toFloat
    
    if (Wrench.holdsApplicableWrench(player, pos)) {
      val sideToToggle = if (player.isShiftKeyDown) side.getOpposite else side
      world.getBlockEntity(pos) match {
        case adapter: tileentity.Adapter =>
          if (!world.isClientSide) {
            val oldValue = adapter.openSides(sideToToggle.ordinal())
            adapter.setSideOpen(sideToToggle, !oldValue)
          }
          InteractionResult.SUCCESS
        case _ => InteractionResult.PASS
      }
    }
    else super.use(state, world, pos, player, hand, hit)
  }
}

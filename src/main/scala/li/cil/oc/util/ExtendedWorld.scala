package li.cil.oc.util

import li.cil.oc.api.network.EnvironmentHost
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.Direction
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level

import scala.language.implicitConversions

object ExtendedWorld {

  implicit def extendedBlockAccess(world: BlockGetter): ExtendedBlockAccess = new ExtendedBlockAccess(world)

  implicit def extendedWorld(world: Level): ExtendedWorld = new ExtendedWorld(world)

  class ExtendedBlockAccess(val world: BlockGetter) {
    def getBlock(position: BlockPosition) = world.getBlockState(position.toBlockPos).getBlock

    def getBlockMapColor(position: BlockPosition) = getBlockMetadata(position).getMapColor(world, position.toBlockPos)

    def getBlockMetadata(position: BlockPosition) = world.getBlockState(position.toBlockPos)

    def getTileEntity(position: BlockPosition): BlockEntity = world.getBlockEntity(position.toBlockPos)

    def getTileEntity(host: EnvironmentHost): BlockEntity = getTileEntity(BlockPosition(host))

    def isAirBlock(position: BlockPosition) = world.getBlockState(position.toBlockPos).isAir

    def getLightBrightnessForSkyBlocks(position: BlockPosition, minBrightness: Int) = world.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, position.toBlockPos)
  }

  class ExtendedWorld(override val world: Level) extends ExtendedBlockAccess(world) {
    def blockExists(position: BlockPosition) = world.isBlockLoaded(position.toBlockPos)

    def breakBlock(position: BlockPosition, drops: Boolean = true) = world.destroyBlock(position.toBlockPos, drops, null)

    def destroyBlockInWorldPartially(entityId: Int, position: BlockPosition, progress: Int) = world.sendBlockBreakProgress(entityId, position.toBlockPos, progress)

    def extinguishFire(player: Player, position: BlockPosition, side: Direction) = world.removeBlock(position.toBlockPos, false)

    def getBlockHardness(position: BlockPosition) = world.getBlockState(position.toBlockPos).getDestroySpeed(world, position.toBlockPos)

    def getBlockHarvestLevel(position: BlockPosition) = 0 // Harvest levels removed in 1.20.1

    def getBlockHarvestTool(position: BlockPosition) = "" // Harvest tools changed in 1.20.1

    def computeRedstoneSignal(position: BlockPosition, side: Direction) = math.max(world.getDirectSignalTo(position.offset(side)), world.getBestNeighborSignal(position.offset(side)))

    def isBlockProvidingPowerTo(position: BlockPosition, side: Direction) = world.getDirectSignalTo(position.toBlockPos)

    def getIndirectPowerLevelTo(position: BlockPosition, side: Direction) = world.getBestNeighborSignal(position.toBlockPos)

    def notifyBlockUpdate(pos: BlockPos): Unit = world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3)

    def notifyBlockUpdate(position: BlockPosition): Unit = notifyBlockUpdate(position, world.getBlockState(position.toBlockPos), world.getBlockState(position.toBlockPos))

    def notifyBlockUpdate(position: BlockPosition, oldState: BlockState, newState: BlockState, flags: Int = 3): Unit = world.sendBlockUpdated(position.toBlockPos, oldState, newState, flags)

    def notifyBlockOfNeighborChange(position: BlockPosition, block: Block) = world.neighborChanged(position.toBlockPos, block, position.toBlockPos)

    def notifyBlocksOfNeighborChange(position: BlockPosition, block: Block, updateObservers: Boolean) = world.updateNeighborsAt(position.toBlockPos, block)

    def notifyBlocksOfNeighborChange(position: BlockPosition, block: Block, side: Direction) = world.updateNeighborsAtExceptFromFacing(position.toBlockPos, block, side)

    def playAuxSFX(id: Int, position: BlockPosition, data: Int) = world.levelEvent(id, position.toBlockPos, data)

    def setBlock(position: BlockPosition, block: Block) = world.setBlock(position.toBlockPos, block.defaultBlockState(), 3)

    def setBlock(position: BlockPosition, block: Block, metadata: Int, flag: Int) = world.setBlock(position.toBlockPos, block.defaultBlockState(), flag)

    def setBlockToAir(position: BlockPosition) = world.removeBlock(position.toBlockPos, false)

    def isSideSolid(position: BlockPosition, side: Direction) = world.getBlockState(position.toBlockPos).isFaceSturdy(world, position.toBlockPos, side)

    def isBlockLoaded(position: BlockPosition) = world.isBlockLoaded(position.toBlockPos)
  }

}

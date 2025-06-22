package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ItemColorizer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.client.model.data.ModelProperty

import scala.jdk.CollectionConverters._
import scala.collection.mutable

object CableModel extends CableModel {
  // ModelData properties to replace ExtendedBlockState
  val NEIGHBORS_PROPERTY: ModelProperty[Integer] = new ModelProperty[Integer]()
  val COLOR_PROPERTY: ModelProperty[Integer] = new ModelProperty[Integer]()
  val IS_SIDE_CABLE_PROPERTY: ModelProperty[Integer] = new ModelProperty[Integer]()
}

class CableModel extends SmartBlockModelBase {
  import CableModel._
  
  override def getOverrides: ItemOverrides = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource): util.List[BakedQuad] = {
    // Default implementation without ModelData - returns basic cable model
    val faces = mutable.ArrayBuffer.empty[BakedQuad]
    faces ++= bakeQuads(Middle, cableTexture, Color.rgbValues(DyeColor.LIGHT_GRAY)).toSeq
    faces.asJava
  }
  
  override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource, extraData: ModelData): util.List[BakedQuad] = {
    val neighbors = extraData.get(NEIGHBORS_PROPERTY)
    val color = extraData.get(COLOR_PROPERTY) 
    val isCableSide = extraData.get(IS_SIDE_CABLE_PROPERTY)
    
    (neighbors, color, isCableSide) match {
      case (neighbourMask: Integer, cableColor: Integer, isCableOnSideMask: Integer) =>
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        faces ++= bakeQuads(Middle, cableTexture, cableColor).toSeq
        for (direction <- Direction.values) {
          val connected = (neighbourMask & (1 << direction.get3DDataValue())) != 0
          val isCableOnSide = (isCableOnSideMask & (1 << direction.get3DDataValue())) != 0
          val (plug, shortBody, longBody) = Connected(direction.get3DDataValue())
          if (connected) {
            if (isCableOnSide) {
              faces ++= bakeQuads(longBody, cableTexture, cableColor).toSeq
            }
            else {
              faces ++= bakeQuads(shortBody, cableTexture, cableColor).toSeq
              faces ++= bakeQuads(plug, cableCapTexture, Color.rgbValues(DyeColor.LIGHT_GRAY)).toSeq
            }
          }
          else if (((1 << direction.getOpposite.get3DDataValue()) & neighbourMask) == neighbourMask || neighbourMask == 0) {
            faces ++= bakeQuads(Disconnected(direction.get3DDataValue()), cableCapTexture, Color.rgbValues(DyeColor.LIGHT_GRAY)).toSeq
          }
        }

        faces.asJava
      case _ => getQuads(state, side, rand)
    }
  }

  protected def isCable(world: BlockGetter, pos: BlockPos) = {
    world.getBlockEntity(pos).isInstanceOf[tileentity.Cable]
  }

  protected final val Middle = makeBox(new Vec3(6 / 16f, 6 / 16f, 6 / 16f), new Vec3(10 / 16f, 10 / 16f, 10 / 16f))

  // Per side, always plug + short cable + long cable (no plug).
  protected final val Connected = Array(
    (makeBox(new Vec3(5 / 16f, 0 / 16f, 5 / 16f), new Vec3(11 / 16f, 1 / 16f, 11 / 16f)),
      makeBox(new Vec3(6 / 16f, 1 / 16f, 6 / 16f), new Vec3(10 / 16f, 6 / 16f, 10 / 16f)),
      makeBox(new Vec3(6 / 16f, 0 / 16f, 6 / 16f), new Vec3(10 / 16f, 6 / 16f, 10 / 16f))),
    (makeBox(new Vec3(5 / 16f, 15 / 16f, 5 / 16f), new Vec3(11 / 16f, 16 / 16f, 11 / 16f)),
      makeBox(new Vec3(6 / 16f, 10 / 16f, 6 / 16f), new Vec3(10 / 16f, 15 / 16f, 10 / 16f)),
      makeBox(new Vec3(6 / 16f, 10 / 16f, 6 / 16f), new Vec3(10 / 16f, 16 / 16f, 10 / 16f))),
    (makeBox(new Vec3(5 / 16f, 5 / 16f, 0 / 16f), new Vec3(11 / 16f, 11 / 16f, 1 / 16f)),
      makeBox(new Vec3(6 / 16f, 6 / 16f, 1 / 16f), new Vec3(10 / 16f, 10 / 16f, 6 / 16f)),
      makeBox(new Vec3(6 / 16f, 6 / 16f, 0 / 16f), new Vec3(10 / 16f, 10 / 16f, 6 / 16f))),
    (makeBox(new Vec3(5 / 16f, 5 / 16f, 15 / 16f), new Vec3(11 / 16f, 11 / 16f, 16 / 16f)),
      makeBox(new Vec3(6 / 16f, 6 / 16f, 10 / 16f), new Vec3(10 / 16f, 10 / 16f, 15 / 16f)),
      makeBox(new Vec3(6 / 16f, 6 / 16f, 10 / 16f), new Vec3(10 / 16f, 10 / 16f, 16 / 16f))),
    (makeBox(new Vec3(0 / 16f, 5 / 16f, 5 / 16f), new Vec3(1 / 16f, 11 / 16f, 11 / 16f)),
      makeBox(new Vec3(1 / 16f, 6 / 16f, 6 / 16f), new Vec3(6 / 16f, 10 / 16f, 10 / 16f)),
      makeBox(new Vec3(0 / 16f, 6 / 16f, 6 / 16f), new Vec3(6 / 16f, 10 / 16f, 10 / 16f))),
    (makeBox(new Vec3(15 / 16f, 5 / 16f, 5 / 16f), new Vec3(16 / 16f, 11 / 16f, 11 / 16f)),
      makeBox(new Vec3(10 / 16f, 6 / 16f, 6 / 16f), new Vec3(15 / 16f, 10 / 16f, 10 / 16f)),
      makeBox(new Vec3(10 / 16f, 6 / 16f, 6 / 16f), new Vec3(16 / 16f, 10 / 16f, 10 / 16f)))
  )

  // Per side, cap only.
  protected final val Disconnected = Array(
    makeBox(new Vec3(6 / 16f, 5 / 16f, 6 / 16f), new Vec3(10 / 16f, 6 / 16f, 10 / 16f)),
    makeBox(new Vec3(6 / 16f, 10 / 16f, 6 / 16f), new Vec3(10 / 16f, 11 / 16f, 10 / 16f)),
    makeBox(new Vec3(6 / 16f, 6 / 16f, 5 / 16f), new Vec3(10 / 16f, 10 / 16f, 6 / 16f)),
    makeBox(new Vec3(6 / 16f, 6 / 16f, 10 / 16f), new Vec3(10 / 16f, 10 / 16f, 11 / 16f)),
    makeBox(new Vec3(5 / 16f, 6 / 16f, 6 / 16f), new Vec3(6 / 16f, 10 / 16f, 10 / 16f)),
    makeBox(new Vec3(10 / 16f, 6 / 16f, 6 / 16f), new Vec3(11 / 16f, 10 / 16f, 10 / 16f))
  )

  protected def cableTexture = Array.fill(6)(Textures.getSprite(Textures.Block.Cable))

  protected def cableCapTexture = Array.fill(6)(Textures.getSprite(Textures.Block.CableCap))

  object ItemOverride extends ItemOverrides {
    class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
      override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource): util.List[BakedQuad] = {
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        val color = if (ItemColorizer.hasColor(stack)) ItemColorizer.getColor(stack) else Color.rgbValues(DyeColor.LIGHT_GRAY)

        faces ++= bakeQuads(Middle, cableTexture, color).toSeq
        faces ++= bakeQuads(Connected(0)._2, cableTexture, color).toSeq
        faces ++= bakeQuads(Connected(1)._2, cableTexture, color).toSeq
        faces ++= bakeQuads(Connected(0)._1, cableCapTexture, Color.rgbValues(DyeColor.LIGHT_GRAY)).toSeq
        faces ++= bakeQuads(Connected(1)._1, cableCapTexture, Color.rgbValues(DyeColor.LIGHT_GRAY)).toSeq

        faces.asJava
      }
    }

    override def resolve(originalModel: BakedModel, stack: ItemStack, level: Level, entity: LivingEntity, seed: Int): BakedModel = new ItemModel(stack)
  }

}
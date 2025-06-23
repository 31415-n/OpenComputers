package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import com.google.common.base.Strings
import li.cil.oc.Settings
import li.cil.oc.client.KeyBindings
import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraftforge.client.model.data.ModelData

import scala.jdk.CollectionConverters._
import scala.collection.mutable

object PrintModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrides = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource): util.List[BakedQuad] = {
    val faces = mutable.ArrayBuffer.empty[BakedQuad]
    // Default empty model - actual rendering will be handled through ModelData
    faces.asJava
  }

  override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource, data: ModelData, renderType: net.minecraft.client.renderer.RenderType): util.List[BakedQuad] = {
    val printEntity = data.get(li.cil.oc.common.block.property.PropertyTile.TILE_ENTITY_PROPERTY)
    printEntity match {
      case t: tileentity.Print =>
        val faces = mutable.ArrayBuffer.empty[BakedQuad]

        for (shape <- t.shapes if !Strings.isNullOrEmpty(shape.texture)) {
          val bounds = shape.bounds.rotateTowards(t.facing)
          val texture = resolveTexture(shape.texture)
          faces ++= bakeQuads(makeBox(bounds.min, bounds.max), Array.fill(6)(texture), shape.tint.getOrElse(White)).toSeq
        }

        faces.asJava
      case _ => getQuads(state, side, rand)
    }
  }

  private def resolveTexture(name: String) = {
    val texture = Textures.getSprite(name)
    if (texture.contents().name().toString == "missingno") Textures.getSprite("minecraft:block/" + name)
    else texture
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val data = new PrintData(stack)

    override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource): util.List[BakedQuad] = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      val shapes =
        if (data.hasActiveState && KeyBindings.showExtendedTooltips)
          data.stateOn
        else
          data.stateOff
      for (shape <- shapes) {
        val bounds = shape.bounds
        val texture = resolveTexture(shape.texture)
        faces ++= bakeQuads(makeBox(bounds.min, bounds.max), Array.fill(6)(texture), shape.tint.getOrElse(White)).toSeq
      }
      if (shapes.isEmpty) {
        val bounds = ExtendedAABB.unitBounds
        val texture = resolveTexture(Settings.resourceDomain + ":block/white")
        faces ++= bakeQuads(makeBox(bounds.min, bounds.max), Array.fill(6)(texture), Color.rgbValues(DyeColor.LIME)).toSeq
      }

      faces.asJava
    }
  }

  object ItemOverride extends ItemOverrides {
    override def resolve(originalModel: BakedModel, stack: ItemStack, world: Level, entity: LivingEntity, seed: Int): BakedModel = new ItemModel(stack)
  }

}

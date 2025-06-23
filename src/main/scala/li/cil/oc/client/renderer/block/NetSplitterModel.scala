package li.cil.oc.client.renderer.block

import java.util
import java.util.Collections

import li.cil.oc.client.Textures
import li.cil.oc.common.block
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.renderer.block.model.ItemOverrides
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import net.minecraftforge.client.event.TextureStitchEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.client.model.data.ModelData

import scala.jdk.CollectionConverters._
import scala.collection.mutable

object NetSplitterModel extends SmartBlockModelBase {
  override def getOverrides: ItemOverrides = ItemOverride

  override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource): util.List[BakedQuad] = {
    val faces = mutable.ArrayBuffer.empty[BakedQuad]
    faces ++= BaseModel
    addSideQuads(faces, Direction.values().map(_ => false))
    faces.asJava
  }

  override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource, data: net.minecraftforge.client.model.data.ModelData, renderType: net.minecraft.client.renderer.RenderType): util.List[BakedQuad] = {
    val netSplitter = data.get(li.cil.oc.common.block.property.PropertyTile.TILE_ENTITY_PROPERTY)
    netSplitter match {
      case splitter: tileentity.NetSplitter =>
        val faces = mutable.ArrayBuffer.empty[BakedQuad]
        faces ++= BaseModel
        addSideQuads(faces, Direction.values().map(splitter.isSideOpen))
        faces.asJava
      case _ => getQuads(state, side, rand)
    }
  }

  protected def splitterTexture = Array(
    Textures.getSprite(Textures.Block.NetSplitterTop),
    Textures.getSprite(Textures.Block.NetSplitterTop),
    Textures.getSprite(Textures.Block.NetSplitterSide),
    Textures.getSprite(Textures.Block.NetSplitterSide),
    Textures.getSprite(Textures.Block.NetSplitterSide),
    Textures.getSprite(Textures.Block.NetSplitterSide)
  )

  protected def GenerateBaseModel() = {
    val faces = mutable.ArrayBuffer.empty[BakedQuad]

    // Bottom.
    faces ++= bakeQuads(makeBox(new Vec3(0 / 16f, 0 / 16f, 5 / 16f), new Vec3(5 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(11 / 16f, 0 / 16f, 5 / 16f), new Vec3(16 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 0 / 16f, 0 / 16f), new Vec3(11 / 16f, 5 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 0 / 16f, 11 / 16f), new Vec3(11 / 16f, 5 / 16f, 16 / 16f)), splitterTexture, None)
    // Corners.
    faces ++= bakeQuads(makeBox(new Vec3(0 / 16f, 0 / 16f, 0 / 16f), new Vec3(5 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(11 / 16f, 0 / 16f, 0 / 16f), new Vec3(16 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(0 / 16f, 0 / 16f, 11 / 16f), new Vec3(5 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(11 / 16f, 0 / 16f, 11 / 16f), new Vec3(16 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)
    // Top.
    faces ++= bakeQuads(makeBox(new Vec3(0 / 16f, 11 / 16f, 5 / 16f), new Vec3(5 / 16f, 16 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(11 / 16f, 11 / 16f, 5 / 16f), new Vec3(16 / 16f, 16 / 16f, 11 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 11 / 16f, 0 / 16f), new Vec3(11 / 16f, 16 / 16f, 5 / 16f)), splitterTexture, None)
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 11 / 16f, 11 / 16f), new Vec3(11 / 16f, 16 / 16f, 16 / 16f)), splitterTexture, None)

    faces.toArray
  }

  protected var BaseModel = Array.empty[BakedQuad]

  @SubscribeEvent
  def onTextureStitch(e: TextureStitchEvent.Post): Unit = {
    BaseModel = GenerateBaseModel()
  }

  protected def addSideQuads(faces: mutable.ArrayBuffer[BakedQuad], openSides: Array[Boolean]): Unit = {
    val down = openSides(Direction.DOWN.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, if (down) 0 / 16f else 2 / 16f, 5 / 16f), new Vec3(11 / 16f, 5 / 16f, 11 / 16f)), splitterTexture, None)

    val up = openSides(Direction.UP.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 11 / 16f, 5 / 16f), new Vec3(11 / 16f, if (up) 16 / 16f else 14f / 16f, 11 / 16f)), splitterTexture, None)

    val north = openSides(Direction.NORTH.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 5 / 16f, if (north) 0 / 16f else 2 / 16f), new Vec3(11 / 16f, 11 / 16f, 5 / 16f)), splitterTexture, None)

    val south = openSides(Direction.SOUTH.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(5 / 16f, 5 / 16f, 11 / 16f), new Vec3(11 / 16f, 11 / 16f, if (south) 16 / 16f else 14 / 16f)), splitterTexture, None)

    val west = openSides(Direction.WEST.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(if (west) 0 / 16f else 2 / 16f, 5 / 16f, 5 / 16f), new Vec3(5 / 16f, 11 / 16f, 11 / 16f)), splitterTexture, None)

    val east = openSides(Direction.EAST.ordinal())
    faces ++= bakeQuads(makeBox(new Vec3(11 / 16f, 5 / 16f, 5 / 16f), new Vec3(if (east) 16 / 16f else 14 / 16f, 11 / 16f, 11 / 16f)), splitterTexture, None)
  }

  class ItemModel(val stack: ItemStack) extends SmartBlockModelBase {
    val data = new PrintData(stack)

    override def getQuads(state: BlockState, side: Direction, rand: net.minecraft.util.RandomSource): util.List[BakedQuad] = {
      val faces = mutable.ArrayBuffer.empty[BakedQuad]

      Textures.Block.bind()

      faces ++= BaseModel
      addSideQuads(faces, Direction.values().map(_ => false))

      faces.asJava
    }
  }

  object ItemOverride extends ItemOverrides {
    override def resolve(originalModel: BakedModel, stack: ItemStack, world: Level, entity: LivingEntity, seed: Int): BakedModel = new ItemModel(stack)
  }

}

package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.core.Direction
import org.lwjgl.opengl.GL11

/**
 * Adapter block entity renderer for 1.20.1.
 * Ported from 1.12.2 TileEntitySpecialRenderer system to modern BlockEntityRenderer.
 * 
 * Original functionality:
 * - Renders adapter side activity indicators
 * - Shows which sides are open/active
 * - Uses proper texture mapping and vertex rendering
 */
class AdapterRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[tileentity.Adapter] {

  override def render(adapter: tileentity.Adapter, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    if (adapter.openSides.contains(true)) {
      RenderState.pushAttrib()
      RenderState.disableEntityLighting()
      RenderState.makeItBlend()

      poseStack.pushPose()

      // Transform to center of block (equivalent to GlStateManager.translate and scale)
      poseStack.translate(0.5, 0.5, 0.5)
      poseStack.scale(1.0025f, -1.0025f, 1.0025f)
      poseStack.translate(-0.5f, -0.5f, -0.5f)

      // Get vertex consumer for rendering
      val vertexConsumer = bufferSource.getBuffer(RenderType.cutout())

      val sideActivity = Textures.getSprite(Textures.Block.AdapterOn)

      // Render each open side with activity texture
      renderSide(adapter, vertexConsumer, poseStack, Direction.DOWN, sideActivity, packedLight, packedOverlay)
      renderSide(adapter, vertexConsumer, poseStack, Direction.UP, sideActivity, packedLight, packedOverlay)
      renderSide(adapter, vertexConsumer, poseStack, Direction.NORTH, sideActivity, packedLight, packedOverlay)
      renderSide(adapter, vertexConsumer, poseStack, Direction.SOUTH, sideActivity, packedLight, packedOverlay)
      renderSide(adapter, vertexConsumer, poseStack, Direction.WEST, sideActivity, packedLight, packedOverlay)
      renderSide(adapter, vertexConsumer, poseStack, Direction.EAST, sideActivity, packedLight, packedOverlay)

      RenderState.disableBlend()
      RenderState.enableEntityLighting()

      poseStack.popPose()
      RenderState.popAttrib()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  /**
   * Render a single side of the adapter if it's open.
   */
  private def renderSide(adapter: tileentity.Adapter, vertexConsumer: VertexConsumer, poseStack: PoseStack, side: Direction, sprite: net.minecraft.client.renderer.texture.TextureAtlasSprite, packedLight: Int, packedOverlay: Int): Unit = {
    if (adapter.isSideOpen(side)) {
      val matrix = poseStack.last().pose()
      
      side match {
        case Direction.DOWN =>
          vertexConsumer.vertex(matrix, 0, 1, 0).uv(sprite.getU1, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, -1, 0).endVertex()
          vertexConsumer.vertex(matrix, 1, 1, 0).uv(sprite.getU0, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, -1, 0).endVertex()
          vertexConsumer.vertex(matrix, 1, 1, 1).uv(sprite.getU0, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, -1, 0).endVertex()
          vertexConsumer.vertex(matrix, 0, 1, 1).uv(sprite.getU1, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, -1, 0).endVertex()
          
        case Direction.UP =>
          vertexConsumer.vertex(matrix, 0, 0, 0).uv(sprite.getU1, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex()
          vertexConsumer.vertex(matrix, 0, 0, 1).uv(sprite.getU1, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex()
          vertexConsumer.vertex(matrix, 1, 0, 1).uv(sprite.getU0, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex()
          vertexConsumer.vertex(matrix, 1, 0, 0).uv(sprite.getU0, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 1, 0).endVertex()
          
        case Direction.NORTH =>
          vertexConsumer.vertex(matrix, 1, 1, 0).uv(sprite.getU0, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, -1).endVertex()
          vertexConsumer.vertex(matrix, 0, 1, 0).uv(sprite.getU1, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, -1).endVertex()
          vertexConsumer.vertex(matrix, 0, 0, 0).uv(sprite.getU1, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, -1).endVertex()
          vertexConsumer.vertex(matrix, 1, 0, 0).uv(sprite.getU0, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, -1).endVertex()
          
        case Direction.SOUTH =>
          vertexConsumer.vertex(matrix, 0, 1, 1).uv(sprite.getU0, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex()
          vertexConsumer.vertex(matrix, 1, 1, 1).uv(sprite.getU1, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex()
          vertexConsumer.vertex(matrix, 1, 0, 1).uv(sprite.getU1, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex()
          vertexConsumer.vertex(matrix, 0, 0, 1).uv(sprite.getU0, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(0, 0, 1).endVertex()
          
        case Direction.WEST =>
          vertexConsumer.vertex(matrix, 0, 1, 0).uv(sprite.getU0, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(-1, 0, 0).endVertex()
          vertexConsumer.vertex(matrix, 0, 1, 1).uv(sprite.getU1, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(-1, 0, 0).endVertex()
          vertexConsumer.vertex(matrix, 0, 0, 1).uv(sprite.getU1, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(-1, 0, 0).endVertex()
          vertexConsumer.vertex(matrix, 0, 0, 0).uv(sprite.getU0, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(-1, 0, 0).endVertex()
          
        case Direction.EAST =>
          vertexConsumer.vertex(matrix, 1, 1, 1).uv(sprite.getU0, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(1, 0, 0).endVertex()
          vertexConsumer.vertex(matrix, 1, 1, 0).uv(sprite.getU1, sprite.getV1).overlayCoords(packedOverlay).uv2(packedLight).normal(1, 0, 0).endVertex()
          vertexConsumer.vertex(matrix, 1, 0, 0).uv(sprite.getU1, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(1, 0, 0).endVertex()
          vertexConsumer.vertex(matrix, 1, 0, 1).uv(sprite.getU0, sprite.getV0).overlayCoords(packedOverlay).uv2(packedLight).normal(1, 0, 0).endVertex()
      }
    }
  }
}

/**
 * Companion object for creating the renderer
 */
object AdapterRenderer {
  def apply(context: BlockEntityRendererProvider.Context): AdapterRenderer = new AdapterRenderer(context)
}
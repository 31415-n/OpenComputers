package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Charger
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.core.Direction
import org.joml.Matrix4f

class ChargerRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Charger] {
  override def render(charger: Charger, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    if (charger.chargeSpeed > 0) {
      poseStack.pushPose()
      poseStack.translate(0.5, 0.5, 0.5)

      charger.yaw match {
        case Direction.WEST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(-90).toFloat))
        case Direction.NORTH => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(180).toFloat))
        case Direction.EAST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(90).toFloat))
        case _ => // No yaw.
      }

      poseStack.translate(-0.5f, 0.5f, 0.5f)
      poseStack.scale(1, -1, 1)

      val buffer = bufferSource.getBuffer(RenderType.cutout())
      val pose = poseStack.last().pose()

      {
        val inverse = 1 - charger.chargeSpeed
        val icon = Textures.getSprite(Textures.Block.ChargerFrontOn)
        val vInterp = icon.getV(inverse * 16)
        
        addVertex(buffer, pose, 0, 1, 0.005f, icon.getU0, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 1, 0.005f, icon.getU1, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, inverse.toFloat, 0.005f, icon.getU1, vInterp, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, inverse.toFloat, 0.005f, icon.getU0, vInterp, packedLight, packedOverlay)
      }

      if (charger.hasPower) {
        val icon = Textures.getSprite(Textures.Block.ChargerSideOn)

        // Left side
        addVertex(buffer, pose, -0.005f, 1, -1, icon.getU0, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, -0.005f, 1, 0, icon.getU1, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, -0.005f, 0, 0, icon.getU1, icon.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, -0.005f, 0, -1, icon.getU0, icon.getV0, packedLight, packedOverlay)

        // Back side
        addVertex(buffer, pose, 1, 1, -1.005f, icon.getU0, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, 1, -1.005f, icon.getU1, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 0, 0, -1.005f, icon.getU1, icon.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 1, 0, -1.005f, icon.getU0, icon.getV0, packedLight, packedOverlay)

        // Right side
        addVertex(buffer, pose, 1.005f, 1, 0, icon.getU0, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 1.005f, 1, -1, icon.getU1, icon.getV1, packedLight, packedOverlay)
        addVertex(buffer, pose, 1.005f, 0, -1, icon.getU1, icon.getV0, packedLight, packedOverlay)
        addVertex(buffer, pose, 1.005f, 0, 0, icon.getU0, icon.getV0, packedLight, packedOverlay)
      }

      poseStack.popPose()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  /**
   * Helper method to add a vertex to the buffer with proper format
   */
  private def addVertex(buffer: VertexConsumer, pose: Matrix4f, x: Float, y: Float, z: Float, u: Float, v: Float, packedLight: Int, packedOverlay: Int): Unit = {
    buffer.vertex(pose, x, y, z)
      .uv(u, v)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 1.0f, 0.0f)
      .endVertex()
  }
}

/**
 * Companion object for creating the renderer
 */
object ChargerRenderer {
  def apply(context: BlockEntityRendererProvider.Context): ChargerRenderer = new ChargerRenderer(context)
}

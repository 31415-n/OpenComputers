package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import org.joml.Matrix4f

class TransposerRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[tileentity.Transposer] {
  override def render(transposer: tileentity.Transposer, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    val activity = math.max(0, 1 - (System.currentTimeMillis() - transposer.lastOperation) / 1000.0)
    if (activity > 0) {
      poseStack.pushPose()
      poseStack.translate(0.5, 0.5, 0.5)
      poseStack.scale(1.0025f, -1.0025f, 1.0025f)
      poseStack.translate(-0.5f, -0.5f, -0.5f)

      val buffer = bufferSource.getBuffer(RenderType.translucent())
      val pose = poseStack.last().pose()
      val alpha = activity.toFloat

      val icon = Textures.getSprite(Textures.Block.TransposerOn)
      // Top face
      addVertexWithAlpha(buffer, pose, 0, 1, 0, icon.getU1, icon.getV0, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 1, 1, 0, icon.getU0, icon.getV0, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 1, 1, 1, icon.getU0, icon.getV1, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 0, 1, 1, icon.getU1, icon.getV1, packedLight, packedOverlay, alpha)

      // Bottom face
      addVertexWithAlpha(buffer, pose, 0, 0, 0, icon.getU1, icon.getV1, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 0, 0, 1, icon.getU1, icon.getV0, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 1, 0, 1, icon.getU0, icon.getV0, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 1, 0, 0, icon.getU0, icon.getV1, packedLight, packedOverlay, alpha)

      // Front face
      addVertexWithAlpha(buffer, pose, 1, 1, 0, icon.getU0, icon.getV1, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 0, 1, 0, icon.getU1, icon.getV1, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 0, 0, 0, icon.getU1, icon.getV0, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 1, 0, 0, icon.getU0, icon.getV0, packedLight, packedOverlay, alpha)

      // Back face
      addVertexWithAlpha(buffer, pose, 0, 1, 1, icon.getU0, icon.getV1, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 1, 1, 1, icon.getU1, icon.getV1, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 1, 0, 1, icon.getU1, icon.getV0, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 0, 0, 1, icon.getU0, icon.getV0, packedLight, packedOverlay, alpha)

      // Left face
      addVertexWithAlpha(buffer, pose, 0, 1, 0, icon.getU0, icon.getV1, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 0, 1, 1, icon.getU1, icon.getV1, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 0, 0, 1, icon.getU1, icon.getV0, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 0, 0, 0, icon.getU0, icon.getV0, packedLight, packedOverlay, alpha)

      // Right face
      addVertexWithAlpha(buffer, pose, 1, 1, 1, icon.getU0, icon.getV1, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 1, 1, 0, icon.getU1, icon.getV1, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 1, 0, 0, icon.getU1, icon.getV0, packedLight, packedOverlay, alpha)
      addVertexWithAlpha(buffer, pose, 1, 0, 1, icon.getU0, icon.getV0, packedLight, packedOverlay, alpha)

      poseStack.popPose()
    }

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def addVertexWithAlpha(buffer: VertexConsumer, pose: Matrix4f, x: Float, y: Float, z: Float, u: Float, v: Float, packedLight: Int, packedOverlay: Int, alpha: Float): Unit = {
    buffer.vertex(pose, x, y, z)
      .color(1.0f, 1.0f, 1.0f, alpha)
      .uv(u, v)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 1.0f, 0.0f)
      .endVertex()
  }
}

object TransposerRenderer {
  def apply(context: BlockEntityRendererProvider.Context): TransposerRenderer = new TransposerRenderer(context)
}

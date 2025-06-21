package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Case
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f

class CaseRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Case] {
  override def render(computer: Case, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    poseStack.pushPose()
    poseStack.translate(0.5, 0.5, 0.5)

    computer.yaw match {
      case Direction.WEST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(-90).toFloat))
      case Direction.NORTH => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(180).toFloat))
      case Direction.EAST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(90).toFloat))
      case _ => // No yaw.
    }

    poseStack.translate(-0.5, 0.5, 0.505)
    poseStack.scale(1, -1, 1)

    val buffer = bufferSource.getBuffer(RenderType.cutout())
    val pose = poseStack.last().pose()

    if (computer.isRunning) {
      renderFrontOverlay(buffer, pose, Textures.Block.CaseFrontOn, packedLight, packedOverlay)
      if (System.currentTimeMillis() - computer.lastFileSystemAccess < 400 && computer.getLevel.random.nextDouble() > 0.1) {
        renderFrontOverlay(buffer, pose, Textures.Block.CaseFrontActivity, packedLight, packedOverlay)
      }
    }
    else if (computer.hasErrored && RenderUtil.shouldShowErrorLight(computer.hashCode)) {
      renderFrontOverlay(buffer, pose, Textures.Block.CaseFrontError, packedLight, packedOverlay)
    }

    poseStack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  /**
   * Renders a front overlay texture on the case
   */
  private def renderFrontOverlay(buffer: VertexConsumer, pose: Matrix4f, texture: ResourceLocation, packedLight: Int, packedOverlay: Int): Unit = {
    val icon = Textures.getSprite(texture)
    
    buffer.vertex(pose, 0, 1, 0)
      .uv(icon.getU0, icon.getV1)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 0.0f, 1.0f)
      .endVertex()
    
    buffer.vertex(pose, 1, 1, 0)
      .uv(icon.getU1, icon.getV1)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 0.0f, 1.0f)
      .endVertex()
    
    buffer.vertex(pose, 1, 0, 0)
      .uv(icon.getU1, icon.getV0)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 0.0f, 1.0f)
      .endVertex()
    
    buffer.vertex(pose, 0, 0, 0)
      .uv(icon.getU0, icon.getV0)
      .overlayCoords(packedOverlay)
      .uv2(packedLight)
      .normal(0.0f, 0.0f, 1.0f)
      .endVertex()
  }
}

/**
 * Companion object for creating the renderer
 */
object CaseRenderer {
  def apply(context: BlockEntityRendererProvider.Context): CaseRenderer = new CaseRenderer(context)
}

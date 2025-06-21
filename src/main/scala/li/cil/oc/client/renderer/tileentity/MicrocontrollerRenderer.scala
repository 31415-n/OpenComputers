package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Microcontroller
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import org.joml.Matrix4f

class MicrocontrollerRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[Microcontroller] {
  override def render(mcu: Microcontroller, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    poseStack.pushPose()
    poseStack.translate(0.5, 0.5, 0.5)

    mcu.yaw match {
      case Direction.WEST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(-90).toFloat))
      case Direction.NORTH => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(180).toFloat))
      case Direction.EAST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(90).toFloat))
      case _ => // No yaw.
    }

    poseStack.translate(-0.5, 0.5, 0.505)
    poseStack.scale(1, -1, 1)

    val buffer = bufferSource.getBuffer(RenderType.cutout())
    val pose = poseStack.last().pose()

    renderFrontOverlay(Textures.Block.MicrocontrollerFrontLight, buffer, pose, packedLight, packedOverlay)

    if (mcu.isRunning) {
      renderFrontOverlay(Textures.Block.MicrocontrollerFrontOn, buffer, pose, packedLight, packedOverlay)
    }
    else if (mcu.hasErrored && RenderUtil.shouldShowErrorLight(mcu.hashCode)) {
      renderFrontOverlay(Textures.Block.MicrocontrollerFrontError, buffer, pose, packedLight, packedOverlay)
    }

    poseStack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }

  private def renderFrontOverlay(texture: ResourceLocation, buffer: VertexConsumer, pose: Matrix4f, packedLight: Int, packedOverlay: Int): Unit = {
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
object MicrocontrollerRenderer {
  def apply(context: BlockEntityRendererProvider.Context): MicrocontrollerRenderer = new MicrocontrollerRenderer(context)
}

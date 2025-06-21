package li.cil.oc.client.renderer.tileentity

import com.mojang.blaze3d.vertex.{PoseStack, VertexConsumer}
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.DiskDrive
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.{MultiBufferSource, RenderType}
import net.minecraft.client.renderer.blockentity.{BlockEntityRenderer, BlockEntityRendererProvider}
import net.minecraft.client.renderer.entity.ItemRenderer
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemDisplayContext
import org.joml.Matrix4f

class DiskDriveRenderer(context: BlockEntityRendererProvider.Context) extends BlockEntityRenderer[DiskDrive] {
  override def render(drive: DiskDrive, partialTick: Float, poseStack: PoseStack, bufferSource: MultiBufferSource, packedLight: Int, packedOverlay: Int): Unit = {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    poseStack.pushPose()
    poseStack.translate(0.5, 0.5, 0.5)

    drive.yaw match {
      case Direction.WEST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(-90).toFloat))
      case Direction.NORTH => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(180).toFloat))
      case Direction.EAST => poseStack.mulPose(org.joml.Quaternionf().rotateY(Math.toRadians(90).toFloat))
      case _ => // No yaw.
    }

    drive.items(0) match {
      case stack if !stack.isEmpty =>
        poseStack.pushPose()
        poseStack.translate(0, 3.5f / 16, 6 / 16f)
        poseStack.mulPose(org.joml.Quaternionf().rotateX(Math.toRadians(90).toFloat))
        poseStack.scale(0.5f, 0.5f, 0.5f)

        // Render the item using the modern item renderer
        val itemRenderer = Minecraft.getInstance().getItemRenderer
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, drive.getLevel, 0)
        
        poseStack.popPose()
      case _ =>
    }

    if (System.currentTimeMillis() - drive.lastAccess < 400 && drive.getLevel.random.nextDouble() > 0.1) {
      poseStack.translate(-0.5, 0.5, 0.505)
      poseStack.scale(1, -1, 1)

      val buffer = bufferSource.getBuffer(RenderType.cutout())
      val pose = poseStack.last().pose()

      val icon = Textures.getSprite(Textures.Block.DiskDriveFrontActivity)
      
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

    poseStack.popPose()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}

/**
 * Companion object for creating the renderer
 */
object DiskDriveRenderer {
  def apply(context: BlockEntityRendererProvider.Context): DiskDriveRenderer = new DiskDriveRenderer(context)
}
